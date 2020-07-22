package com.portgo.util;

import java.io.InputStream;
import  java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.util.Log;

import com.portgo.PortApplication;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 *解析苹果机制下的plist
 * @author chen_weihua
 */
public class PListHelper {
    public static List<Object> readXmlBySAX(InputStream inputStream) {
        try {
                /**【创建解析器】**/
                SAXParserFactory spf = SAXParserFactory.newInstance();
                SAXParser saxParser = spf.newSAXParser();
                PListHanler handler = new PListHanler();
                saxParser.parse(inputStream, handler);
                inputStream.close();
                return handler.getArrayResult();
            } catch (Exception e) {
            e.printStackTrace();
            }
            return null;
        }
        static class PListHanler extends DefaultHandler {

            private LinkedList<Object> list = new LinkedList<Object>();
            //是否为根标签
            private boolean isRootElement = false;
            //标签开始
            private boolean keyElementBegin = false;
            //键
            private String key;
            //值开始
            private boolean valueElementBegin = false;
            //根对象
            private Object root;


            @SuppressWarnings("unchecked")
            public Map getMapResult() {
                return (Map) root;
            }

            @SuppressWarnings("unchecked")
            public List getArrayResult() {
                return (List) root;
            }

            @SuppressWarnings("unchecked")
            @Override
            public void startElement(String uri, String localName, String qName,
                                     Attributes attributes) throws SAXException {
                if ("plist".equals(localName)) {
                    isRootElement = true;
                }else if ("dict".equals(localName)) {
                    if (isRootElement) {
                        list.addFirst(new HashMap());
                        isRootElement = !isRootElement;
                    } else {
                        ArrayList parent = (ArrayList) list.get(0);
                        list.addFirst(new HashMap());
                        parent.add(list.get(0));
                    }
                }else if ("key".equals(localName)) {
                    keyElementBegin = true;
                }else if ("true".equals(localName)) {
                    HashMap parent = (HashMap) list.get(0);
                    parent.put(key, true);
                }else if ("false".equals(localName)) {
                    HashMap parent = (HashMap) list.get(0);
                    parent.put(key, false);

                }else if ("array".equals(localName)) {
                    if (isRootElement) {
                        ArrayList obj = new ArrayList();
                        list.addFirst(obj);
                        isRootElement = !isRootElement;
                    } else {
                        HashMap parent = (HashMap) list.get(0);
                        ArrayList obj = new ArrayList();
                        list.addFirst(obj);
                        parent.put(key, obj);
                    }

                }else if ("string".equals(localName)) {
                    valueElementBegin = true;
                }

            }

            @SuppressWarnings("unchecked")
            @Override
            public void characters(char[] ch, int start, int length)
                    throws SAXException {
                if (length > 0) {
                    if (keyElementBegin) {
                        key = new String(ch, start, length);

                    }

                    if (valueElementBegin) {
                        if (HashMap.class.equals(list.get(0).getClass())) {
                            HashMap parent = (HashMap) list.get(0);
                            String value = new String(ch, start, length);
                            if(value.equalsIgnoreCase("Shocked")){
                            }

                            parent.put(key, value);
                        } else if (ArrayList.class.equals(list.get(0).getClass())) {
                            ArrayList parent = (ArrayList) list.get(0);
                            String value = new String(ch, start, length);
                            parent.add(value);
                        }
                    }
                }
            }


            @Override
            public void endElement(String uri, String localName, String qName)
                    throws SAXException {
                if ("plist".equals(localName)) {
                    ;
                }else if ("key".equals(localName)) {
                    keyElementBegin = false;
                }else if ("string".equals(localName)) {
                    valueElementBegin = false;
                }else if ("array".equals(localName)) {
                    root = list.removeFirst();
                }else if ("dict".equals(localName)) {
                    root = list.removeFirst();
                }

            }
        }
}
