package org.openrepose.filters.herp

import java.net.{URL, URLDecoder}
import java.nio.charset.StandardCharsets
import javax.servlet._
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import com.github.jknack.handlebars.Handlebars
import com.hazelcast.config.{Config, QueueConfig, QueueStoreConfig}
import com.hazelcast.core.{Hazelcast, TransactionalQueue}
import com.hazelcast.transaction.TransactionOptions
import com.hazelcast.transaction.TransactionOptions.TransactionType
import com.rackspace.httpdelegation._
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.commons.utils.http.{CommonHttpHeader, HttpStatusCode, OpenStackServiceHeader}
import org.openrepose.core.filter.FilterConfigHelper
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.services.context.ServletContextHelper
import org.openrepose.filters.herp.config.HerpConfig
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._

class HerpFilter extends Filter with HttpDelegationManager with UpdateListener[HerpConfig] with LazyLogging {
  private final val DEFAULT_CONFIG = "highly-efficient-record-processor.cfg.xml"
  private final val X_PROJECT_ID = "X-Project-ID"
  private final val QUEUE_NAME = "audit_queue"

  private var configurationService: ConfigurationService = _
  private var config: String = _
  private var initialized = false
  private var serviceCode: String = _
  private var region: String = _
  private var dataCenter: String = _
  private var auditConsumer: AuditConsumer = _

  // Setting up Hazelcast queue
  private val hcConfig = new Config()
  private val queueConfig = new QueueConfig()
  private val queueStoreConfig = new QueueStoreConfig()
  queueConfig.setQueueStoreConfig(queueStoreConfig)
  hcConfig.addQueueConfig(queueConfig)

  private val transactionOptions = new TransactionOptions().setTransactionType(TransactionType.LOCAL)
  private val hazelcast = Hazelcast.newHazelcastInstance(hcConfig)
  private val transactionContext = hazelcast.newTransactionContext(transactionOptions)
  private val queue: TransactionalQueue[TemplateValues] = transactionContext.getQueue(QUEUE_NAME)

  override def init(filterConfig: FilterConfig): Unit = {
    logger.trace("HERP filter initializing ...")
    config = new FilterConfigHelper(filterConfig).getFilterConfig(DEFAULT_CONFIG)

    logger.info("Initializing filter using config " + config)
    val powerApiContext = ServletContextHelper.getInstance(filterConfig.getServletContext).getPowerApiContext
    configurationService = powerApiContext.configurationService
    val xsdURL: URL = getClass.getResource("/META-INF/schema/config/highly-efficient-record-processor.xsd")
    configurationService.subscribeTo(
      filterConfig.getFilterName,
      config,
      xsdURL,
      this,
      classOf[HerpConfig]
    )

    logger.trace("HERP filter initialized.")
  }

  override def doFilter(servletRequest: ServletRequest, servletResponse: ServletResponse, filterChain: FilterChain): Unit = {
    if (!initialized) {
      logger.error("HERP filter has not yet initialized...")
      servletResponse.asInstanceOf[HttpServletResponse].sendError(500)
    } else {
      logger.trace("HERP filter passing request...")
      filterChain.doFilter(servletRequest, servletResponse)

      logger.trace("HERP filter handling response...")
      handleResponse(servletRequest.asInstanceOf[HttpServletRequest], servletResponse.asInstanceOf[HttpServletResponse])

      logger.trace("HERP filter returning response...")
    }
  }

  private def handleResponse(httpServletRequest: HttpServletRequest,
                             httpServletResponse: HttpServletResponse) = {
    def translateParameters(): Map[String, Array[String]] = {
      def decode(s: String) = URLDecoder.decode(s, StandardCharsets.UTF_8.name())

      Option(httpServletRequest.getAttribute("http://openrepose.org/queryParams")) match {
        case Some(parameters) =>
          val parametersMap = parameters.asInstanceOf[java.util.Map[String, Array[String]]].asScala
          parametersMap.map({ case (key, values) => decode(key) -> values.map(value => decode(value))}).toMap
        case None => Map[String, Array[String]]()
      }
    }

    val templateValues = new TemplateValues(
      userName = httpServletRequest.getHeader(OpenStackServiceHeader.USER_NAME.toString),
      impersonatorName = httpServletRequest.getHeader(OpenStackServiceHeader.IMPERSONATOR_NAME.toString),
      projectId = httpServletRequest.getHeaders(OpenStackServiceHeader.TENANT_ID.toString).asScala
        .++(httpServletRequest.getHeaders(X_PROJECT_ID).asScala).asJava,
      roles = httpServletRequest.getHeaders(OpenStackServiceHeader.ROLES.toString).asScala.asJava,
      userAgent = httpServletRequest.getHeader(CommonHttpHeader.USER_AGENT.toString),
      requestMethod = httpServletRequest.getMethod,
      requestUrl = Option(httpServletRequest.getAttribute("http://openrepose.org/requestUrl")).map(_.toString).getOrElse(""),
      requestQueryString = httpServletRequest.getQueryString,
      parameters = translateParameters().asJava.entrySet(),
      timestamp = System.currentTimeMillis(),
      responseCode = httpServletResponse.getStatus,
      responseMessage = HttpStatusCode.fromInt(httpServletResponse.getStatus).name(),
      guid = java.util.UUID.randomUUID.toString,
      serviceCode,
      region,
      dataCenter
    )

    queue.offer(templateValues)
  }

  override def destroy(): Unit = {
    logger.trace("HERP filter destroying ...")
    configurationService.unsubscribeFrom(config, this.asInstanceOf[UpdateListener[_]])

    while (queue.size() != 0) {
      wait(100)
    }
    hazelcast.shutdown()
    auditConsumer.terminate()

    logger.trace("HERP filter destroyed.")
  }

  override def configurationUpdated(config: HerpConfig): Unit = {
    def templateString: String = {
      var templateText = config.getTemplate.getValue.trim
      if (config.getTemplate.isCrush) {
        templateText = templateText.replaceAll("(?m)[ \\t]*(\\r\\n|\\r|\\n)[ \\t]*", " ")
      }
      templateText
    }

    // Read in configuration
    // todo: small window of error due to setting fields since a request could come in while we're in this method
    serviceCode = config.getServiceCode
    region = config.getRegion
    dataCenter = config.getDataCenter

    auditConsumer.terminate()

    val herpLogger = LoggerFactory.getLogger(config.getLoggerName)
    val handlebarsTemplate = new Handlebars().compileInline(templateString) // todo: any value in adding helpers or setting pretty print? (Handlebars)
    auditConsumer = new AuditConsumer(herpLogger, handlebarsTemplate, queue)
    new Thread(auditConsumer).start()

    initialized = true
  }

  override def isInitialized: Boolean = {
    initialized
  }
}
