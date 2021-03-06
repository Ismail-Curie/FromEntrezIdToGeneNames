package org.pmoi.business;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.pmoi.handler.HttpConnector;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author A Salmi
 */
public class NCBIQueryClient {
    private static final Logger LOGGER = LogManager.getRootLogger();

    public String geneNameToEntrezID(String feature) {
        int counter = 0;
        while (true) {
            try {
                LOGGER.info(String.format("Processing feature: [%s]", feature));
                URL url = new URL(String.format("https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?db=gene&term=%s AND Homo sapiens[Organism]&sort=relevance", feature));
                SAXBuilder saxBuilder = new SAXBuilder();
                Document document = saxBuilder.build(url);
                Element ncbiId = document.getRootElement().getChild("IdList");
                Optional<Element> id = ncbiId.getChildren().stream().findFirst();
                return id.map(Element::getText).orElse(null);

            } catch (JDOMException | IOException e) {
                LOGGER.warn(String.format("Unknown error when getting ID. Feature: [%s]. Retrying (%d/%d)", feature, counter + 1, 100));
                if (++counter == MainEntry.MAX_TRIES) {
                    LOGGER.error(String.format("Error getting ID for feature: [%s]. Aborting!", feature));
                    return null;
                }
            }
        }
    }

    public String entrezIDToGeneName(String feature) {
        int counter = 0;
        while (true) {
            try {
                LOGGER.info(String.format("Processing feature: [%s]", feature));
                URL url = new URL(String.format("https://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=gene&id=%s&retmode=text", feature));
                HttpConnector httpConnector = new HttpConnector();
                String ncbiResultContent = httpConnector.getContent(url);
                // the first line always contains the gene name following the format: 1. NAME\n
                //TODO replace this using regex
                ncbiResultContent = ncbiResultContent.split("\n")[1];
                return ncbiResultContent.substring(3);
            } catch (IOException e) {
                LOGGER.warn(String.format("Unknown error when getting feature name. Feature: [%s]. Retrying (%d/%d)", feature, counter + 1, 100));
                if (++counter == MainEntry.MAX_TRIES) {
                    LOGGER.error(String.format("\nError getting name for feature: [%s]. Aborting!", feature));
                    return null;
                }
            }
        }
    }

    public String fetchDescription(String id) {
        int counter = 0;
        while (true) {
            try {
                URL url = new URL(String.format("https://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=gene&id=%s&retmode=text&api_key=%s", id, MainEntry.NCBI_API_KEY));
                HttpConnector httpConnector = new HttpConnector();
                String ncbiResultContent = httpConnector.getContent(url);
                //TODO replace this using regex
                Pattern pattern = Pattern.compile("Name: (.*)(?= \\[)");
                Matcher matcher = pattern.matcher(ncbiResultContent);
                if (matcher.find()) {
                    return matcher.group(1);
                }
                return null;
            } catch (IOException e) {
                if (++counter == MainEntry.MAX_TRIES) {
                    return null;
                }
            }
        }
    }
}
