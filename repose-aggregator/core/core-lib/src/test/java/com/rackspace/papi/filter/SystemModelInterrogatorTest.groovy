package com.rackspace.papi.filter

import com.google.common.base.Optional
import com.rackspace.papi.domain.Port
import com.rackspace.papi.domain.ReposeInstanceInfo
import com.rackspace.papi.model.*
import org.junit.Before
import org.junit.Test

import static org.hamcrest.CoreMatchers.equalTo
import static org.hamcrest.CoreMatchers.instanceOf
import static org.junit.Assert.*
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

public class SystemModelInterrogatorTest {
    private SystemModelInterrogator interrogator

    @Before
    public void setup() throws Exception {
        List<Port> servicePorts = new ArrayList<Port>()
        servicePorts << new Port("http", 8080)
        servicePorts << new Port("https", 8181)

        def reposeInstanceInfo = mock(ReposeInstanceInfo)

        when(reposeInstanceInfo.getPorts()).thenReturn(servicePorts)

        interrogator = new SystemModelInterrogator(reposeInstanceInfo)
    }

    @Test
    public void "when passed a valid system model, getLocalServiceDomain(...) should return a matching cluster"() throws Exception {
        SystemModel sysModel = getValidSystemModel()

        Optional<ReposeCluster> returnedCluster = interrogator.getLocalCluster(sysModel)

        assertTrue(returnedCluster.isPresent())

        ReposeCluster cluster = returnedCluster.get()

        assertThat(cluster.getId(), equalTo("cluster1"))
        assertThat(cluster.getNodes().getNode().get(0).getId(), equalTo("node1"))
        assertThat(cluster.getNodes().getNode().get(0).getHostname(), equalTo("localhost"))
        assertThat(cluster.getNodes().getNode().get(0).getHttpPort(), equalTo(8080))
    }

    @Test
    public void "when passed a system model missing a matching cluster, getLocalServiceDomain(...) should return an absent Optional"() throws Exception {
        SystemModel sysModel = getValidSystemModel()
        sysModel.getReposeCluster().get(0).getNodes().getNode().get(0).setHostname("www.example.com")

        Optional<ReposeCluster> returnedCluster = interrogator.getLocalCluster(sysModel)

        assertFalse(returnedCluster.isPresent())
    }

    @Test
    public void "when passed a valid system model, getLocalHost(...) should return a matching node"() throws Exception {
        SystemModel sysModel = getValidSystemModel()

        Optional<Node> returnedNode = interrogator.getLocalNode(sysModel)

        assertTrue(returnedNode.isPresent())

        Node node = returnedNode.get()

        assertThat(node.getId(), equalTo("node1"))
        assertThat(node.getHostname(), equalTo("localhost"))
        assertThat(node.getHttpPort(), equalTo(8080))
    }

    @Test
    public void "when passed a system model missing a matching node, getLocalHost(...) should return an absent Optional"() throws Exception {
        SystemModel sysModel = getValidSystemModel()
        sysModel.getReposeCluster().get(0).getNodes().getNode().get(0).setHostname("www.example.com")

        Optional<Node> returnedNode = interrogator.getLocalNode(sysModel)

        assertFalse(returnedNode.isPresent())
    }

    @Test
    public void "when passed a valid system model, getDefaultDestination(...) should return a matching default destination"() throws Exception {
        SystemModel sysModel = getValidSystemModel()

        Optional<Destination> returnedDest = interrogator.getDefaultDestination(sysModel)

        assertTrue(returnedDest.isPresent())

        Destination destination = returnedDest.get()

        assertThat(destination.getId(), equalTo("dest1"))
        assertThat(destination.getProtocol(), equalTo("http"))
        assertThat(destination.getId(), equalTo("dest1"))
        assertThat(destination, instanceOf(DestinationEndpoint))
    }

    @Test
    public void "when passed a system model missing a matching default destination, getDefaultDestination(...) should return an absent Optional"() throws Exception {
        SystemModel sysModel = getValidSystemModel()
        sysModel.getReposeCluster().get(0).getNodes().getNode().get(0).setHostname("www.example.com")

        Optional<Destination> returnedDestination = interrogator.getDefaultDestination(sysModel)

        assertFalse(returnedDestination.isPresent())
    }

    @Test
    public void "when nameResolver.lookupName throws UnknownHostException, cluster should be absent"() throws UnknownHostException {
        SystemModel sysModel = getValidSystemModel()
        sysModel.getReposeCluster().get(0).getNodes().getNode().get(0).setHostname("thiswillneverexist")

        Optional<ReposeCluster> returnedCluster = interrogator.getLocalCluster(sysModel)

        assertFalse(returnedCluster.isPresent())
    }

    @Test
    public void "when no service ports are specified, cluster and destinations both do not exist"(){
        interrogator = new SystemModelInterrogator(mock(ReposeInstanceInfo))
        SystemModel sysModel = getValidSystemModel()

        Optional<ReposeCluster> returnedCluster = interrogator.getLocalCluster(sysModel)

        assertFalse(returnedCluster.isPresent())

        Optional<Destination> destination = interrogator.getDefaultDestination(sysModel)

        assertFalse(destination.isPresent())
    }

    @Test
    public void "when no destinations are present, cluster exists but destinations are absent"(){
        SystemModel sysModel = getValidSystemModel()
        sysModel.reposeCluster[0].destinations = new DestinationList()

        Optional<ReposeCluster> returnedCluster = interrogator.getLocalCluster(sysModel)

        assertTrue(returnedCluster.isPresent())

        Optional<Destination> destination = interrogator.getDefaultDestination(sysModel)

        assertFalse(destination.isPresent())

    }

    @Test
    public void "when no clusters are present, cluster and destination are absent"(){
        SystemModel sysModel = getValidSystemModel()
        sysModel.reposeCluster = new ArrayList<ReposeCluster>()

        Optional<ReposeCluster> returnedCluster = interrogator.getLocalCluster(sysModel)

        assertFalse(returnedCluster.isPresent())

        Optional<Destination> destination = interrogator.getDefaultDestination(sysModel)

        assertFalse(destination.isPresent())
    }

    @Test
    public void "when service ports do not contain BOTH HTTP and HTTPS ports, cluster and destination are absent"(){
        //TODO: this test probably doesn't work, because I'm not passing in ports :(
        //NOTE it behaved the same when ports were passed in, this is weird
        def servicePorts = []
        servicePorts << new Port("http", 8080)
        ReposeInstanceInfo rii = new ReposeInstanceInfo()
        rii.setPorts(servicePorts)

        interrogator = new SystemModelInterrogator(rii)

        SystemModel sysModel = getValidSystemModel()

        Optional<ReposeCluster> returnedCluster = interrogator.getLocalCluster(sysModel)

        assertFalse(returnedCluster.isPresent())

        Optional<Destination> destination = interrogator.getDefaultDestination(sysModel)

        assertFalse(destination.isPresent())
    }


    /**
     * @return a valid system model
     */
    private SystemModel getValidSystemModel() {
        ReposeCluster cluster = new ReposeCluster()
        SystemModel sysModel = new SystemModel()

        cluster.setId("cluster1")
        cluster.setNodes(new NodeList())
        cluster.getNodes().getNode() <<
                new Node(id: "node1", hostname: "localhost", httpPort: 8080, httpsPort: 8181)
        cluster.setDestinations(new DestinationList())
        cluster.getDestinations().getEndpoint() << new DestinationEndpoint(
                hostname: "localhost", port: 9090, default: true, id: "dest1", protocol: "http")

        sysModel.getReposeCluster().add(cluster)

        return sysModel
    }
}
