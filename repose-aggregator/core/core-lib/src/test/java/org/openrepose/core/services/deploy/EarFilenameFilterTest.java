package org.openrepose.core.services.deploy;

import java.io.File;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;


@RunWith(Enclosed.class)
public class EarFilenameFilterTest {
   
   public static class WhenLocatingEarFile{
      
      protected File dir = new File("/usr/share/repose/filters");
      protected EarFilenameFilter earFilenameFilter;
      
      @Before
      public void setUp(){
         
         earFilenameFilter = (EarFilenameFilter)EarFilenameFilter.getInstance();
      }
      
      @Test
      public void shouldReturnTrueForValidEarName(){
         
         assertTrue(earFilenameFilter.accept(dir, "filter-bundle.ear"));
      }
      
      @Test
      public void shouldReturnFalseForInvalidEarName(){
         assertFalse(earFilenameFilter.accept(dir, "filter-bunder"));
      }
      
      @Test
      public void shouldReturnFalseForEmptyEarName(){
         assertFalse(earFilenameFilter.accept(dir, ""));
      }
   }
}
