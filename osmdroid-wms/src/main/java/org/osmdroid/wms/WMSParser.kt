package org.osmdroid.wms

import org.osmdroid.wms.DomParserWms111.parse
import org.xml.sax.EntityResolver
import org.xml.sax.InputSource
import org.xml.sax.SAXException
import java.io.IOException
import java.io.InputStream
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory

/**
 * This is the main entry point for working with WMS servers.
 * Sample code<br></br>
 * <pre>
 * HtpURLConnection c = null;
 * InputStream is = null;
 * WMSEndpoint endpoint = null;
 * try {
 * c = (HttpURLConnection) new URL(youEditTextValue).openConnection();
 * is = c.getInputStream();
 * endpoint = WMSParser.parse(is);
 * } catch (Exception ex) {
 * ex.printStackTrace();
 * } finally {
 * if (is != null)
 * try { is.close(); } catch (Exception ex) { }
 * if (c != null)
 * try { c.disconnect(); } catch (Exception ex) { }
 * }
</pre> *
 * created on 8/25/2017.
 * https://github.com/osmdroid/osmdroid/issues/177
 *
 *
 * See also the sample usage in the "Open Map" demo
 *
 * @author Alex O'Ree
 * @since 6.0.0
 */
object WMSParser {
    /**
     * note, the input stream remains open after calling this method, closing it is the caller's problem
     *
     * @param inputStream
     * @return
     * @throws Exception
     */
    @JvmStatic
    @Throws(Exception::class)
    fun parse(inputStream: InputStream?): WMSEndpoint {
        val dbFactory = DocumentBuilderFactory.newInstance()

        val dBuilder = dbFactory.newDocumentBuilder()
        dBuilder.setEntityResolver(object : EntityResolver {
            @Throws(SAXException::class, IOException::class)
            override fun resolveEntity(publicId: String?, systemId: String?): InputSource {
                return InputSource(StringReader(""))
            }
        })
        val doc = dBuilder.parse(inputStream)


        val element = doc.getDocumentElement()
        element.normalize()

        if (element.getNodeName().contains("WMT_MS_Capabilities")) {
            return parse(element)
        } else if (element.getNodeName().contains("WMS_Capabilities")) {
            return parse(element)
        }
        throw IllegalArgumentException("Unknown root element: " + element.getNodeName())
    }
}
