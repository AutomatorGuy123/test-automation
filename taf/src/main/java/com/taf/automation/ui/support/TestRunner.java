package com.taf.automation.ui.support;

import com.taf.automation.ui.support.testng.TestParameterValidator;
import com.thoughtworks.xstream.XStream;
import org.apache.commons.io.FileUtils;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.testng.ITestNGListener;
import org.testng.TestNG;
import org.testng.reporters.Files;
import ru.yandex.qatools.allure.AllureMain;
import ru.yandex.qatools.allure.config.AllureConfig;
import ru.yandex.qatools.commons.model.Environment;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;

public class TestRunner {
    private String resultsFolder;
    private String reportFolder;
    private int port = 8090;

    public TestRunner() {
        TestProperties props = TestProperties.getInstance();
        AllureConfig config = new AllureConfig();
        resultsFolder = config.getResultsDirectory().getAbsolutePath();
        reportFolder = props.getReportFolder().getAbsolutePath();
    }

    public int runTests(List<String> suites) throws IOException {
        for (String suite : suites) {
            InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(suite);
            File file = new File(suite);
            if (is != null) {
                FileUtils.copyInputStreamToFile(is, file);
            } else if (!file.exists()) {
                throw new RuntimeException("Suite file '" + suite + "' does not exists on the file system!");
            }
        }

        TestNG testNg = new TestNG();
        testNg.addListener((ITestNGListener) new TestParameterValidator());
        testNg.setTestSuites(suites);
        testNg.setSuiteThreadPoolSize(1);
        testNg.run();
        saveEnvironment();
        return testNg.getStatus();
    }

    public void generateReport() throws IOException {
        String[] arguments = {resultsFolder, reportFolder};
        AllureMain.main(arguments);
    }

    public void openReport() throws Exception {
        Server server = setUpServer();
        server.start();
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Desktop.getDesktop().browse(new URI("http://localhost:" + getServerPort()));
        }

        server.join();
    }

    private int getServerPort() {
        int serverPort = port;
        String p = System.getProperty("local.server.port");
        if (p != null) {
            serverPort = Integer.parseInt(p);
        }

        return serverPort;
    }

    private Server setUpServer() {
        Server server = new Server(getServerPort());
        ResourceHandler handler = new ResourceHandler();
        handler.setDirectoriesListed(true);
        handler.setWelcomeFiles(new String[]{"index.html"});
        handler.setResourceBase(reportFolder);
        HandlerList handlers = new HandlerList();
        handlers.setHandlers(new Handler[]{handler, new DefaultHandler()});
        server.setStopAtShutdown(true);
        server.setHandler(handlers);
        server.setStopAtShutdown(true);
        return server;
    }

    public void saveEnvironment() throws IOException {
        TestProperties prop = TestProperties.getInstance();
        Environment environment = new Environment().withName("Environment");
        environment.withParameter(prop.getAsParameters());
        XStream xstream = new XStream();
        xstream.addImplicitArray(Environment.class, "parameter", "parameter");
        String xml = xstream.toXML(environment);
        xml = xml.replace("<ru.yandex.qatools.commons.model.Environment>", "<qa:environment xmlns:qa=\"urn:model.commons.qatools.yandex.ru\">");
        xml = xml.replace("</ru.yandex.qatools.commons.model.Environment>", "</qa:environment>");
        xml = xml.replace(" class=\"ru.yandex.qatools.commons.model.Parameter\"", "");
        File file = new File(resultsFolder + "/environment.xml");
        Files.writeFile(xml, file);
    }

    public void deleteResultsFolder() throws IOException {
        File resFolder = new File(resultsFolder);
        if (resFolder.exists()) {
            FileUtils.forceDelete(resFolder);
        }
    }

    public void deleteReportFolder() throws IOException {
        File resFolder = new File(reportFolder);
        if (resFolder.exists()) {
            FileUtils.forceDelete(resFolder);
        }
    }

}
