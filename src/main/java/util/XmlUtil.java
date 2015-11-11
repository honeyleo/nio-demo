package util;

import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

public class XmlUtil {

	public static <T> T readXML(String xml, Class<T> cls) {
		 try {   
			 Reader xmlReader = new StringReader(xml);
			 SAXReader reader = new SAXReader();
			 Document doc = reader.read(xmlReader);
			 Element objElement = doc.getRootElement();
			 Field[] properties = cls.getDeclaredFields();
			 Method setmeth;  
			 T obj=(T)cls.newInstance();
			 for (int j = 0; j < properties.length; j++) {
				 setmeth = obj.getClass().getMethod("set" + properties[j].getName().substring(0, 1).toUpperCase() + properties[j].getName().substring(1),properties[j].getType());   
               setmeth.invoke(obj, objElement.elementText(properties[j].getName()));
			 }
			 return obj;
       } catch (Exception e) {   
           e.printStackTrace();   
       }   
		return null;
	}
	
	public static String writeXmlDocument(Object obj) {   
       try {   
           XMLWriter writer = null;
           OutputFormat format = OutputFormat.createCompactFormat();   
           format.setEncoding("UTF-8");
           Document document = DocumentHelper.createDocument();   
           Class<?> cls = obj.getClass();
           Field[] properties = cls.getDeclaredFields();
           Element dataElement = document.addElement(cls.getSimpleName());
           for (int i = 0; i < properties.length; i++) {          
               Method meth = cls.getMethod("get" + properties[i].getName().substring(0, 1).toUpperCase() + properties[i].getName().substring(1));   
               dataElement.addElement(properties[i].getName()).setText(meth.invoke(obj).toString());   
           }
           StringWriter sw = new StringWriter();
           writer = new XMLWriter(sw, format);   
           writer.write(document);   
           writer.close();  
           return sw.getBuffer().toString();
       } catch (Exception e) {   
           System.err.println("XML文件写入失败");   
       }   
       return null;
   }
	
}
