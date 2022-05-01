package net.minecraft.gametest.framework;

import com.google.common.base.Stopwatch;
import java.io.File;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class JUnitLikeTestReporter implements TestReporter {
    private final Document document;
    private final Element testSuite;
    private final Stopwatch stopwatch;
    private final File destination;

    public JUnitLikeTestReporter(File file) throws ParserConfigurationException {
        this.destination = file;
        this.document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        this.testSuite = this.document.createElement("testsuite");
        Element element = this.document.createElement("testsuite");
        element.appendChild(this.testSuite);
        this.document.appendChild(element);
        this.testSuite.setAttribute("timestamp", DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
        this.stopwatch = Stopwatch.createStarted();
    }

    private Element createTestCase(GameTestInfo test, String name) {
        Element element = this.document.createElement("testcase");
        element.setAttribute("name", name);
        element.setAttribute("classname", test.getStructureName());
        element.setAttribute("time", String.valueOf((double)test.getRunTime() / 1000.0D));
        this.testSuite.appendChild(element);
        return element;
    }

    @Override
    public void onTestFailed(GameTestInfo test) {
        String string = test.getTestName();
        String string2 = test.getError().getMessage();
        Element element;
        if (test.isRequired()) {
            element = this.document.createElement("failure");
            element.setAttribute("message", string2);
        } else {
            element = this.document.createElement("skipped");
            element.setAttribute("message", string2);
        }

        Element element3 = this.createTestCase(test, string);
        element3.appendChild(element);
    }

    @Override
    public void onTestSuccess(GameTestInfo test) {
        String string = test.getTestName();
        this.createTestCase(test, string);
    }

    @Override
    public void finish() {
        this.stopwatch.stop();
        this.testSuite.setAttribute("time", String.valueOf((double)this.stopwatch.elapsed(TimeUnit.MILLISECONDS) / 1000.0D));

        try {
            this.save(this.destination);
        } catch (TransformerException var2) {
            throw new Error("Couldn't save test report", var2);
        }
    }

    public void save(File file) throws TransformerException {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource dOMSource = new DOMSource(this.document);
        StreamResult streamResult = new StreamResult(file);
        transformer.transform(dOMSource, streamResult);
    }
}
