package org.eobjects.hadoopdatacleaner;

import junit.framework.Assert;

import org.apache.hadoop.util.ToolRunner;
import org.eobjects.analyzer.beans.StringAnalyzer;
import org.eobjects.analyzer.beans.transform.ConcatenatorTransformer;
import org.eobjects.analyzer.beans.transform.TokenizerTransformer;
import org.eobjects.analyzer.beans.valuedist.ValueDistributionAnalyzer;
import org.eobjects.analyzer.beans.writers.InsertIntoTableAnalyzer;
import org.eobjects.analyzer.configuration.AnalyzerBeansConfiguration;
import org.eobjects.analyzer.configuration.AnalyzerBeansConfigurationImpl;
import org.eobjects.analyzer.connection.CsvDatastore;
import org.eobjects.analyzer.connection.Datastore;
import org.eobjects.analyzer.connection.DatastoreCatalog;
import org.eobjects.analyzer.connection.DatastoreCatalogImpl;
import org.eobjects.analyzer.descriptors.Descriptors;
import org.eobjects.analyzer.descriptors.SimpleDescriptorProvider;
import org.eobjects.analyzer.job.AnalysisJob;
import org.eobjects.analyzer.job.builder.AnalysisJobBuilder;
import org.eobjects.analyzer.job.builder.AnalyzerJobBuilder;
import org.eobjects.analyzer.job.builder.TransformerJobBuilder;
import org.eobjects.metamodel.csv.CsvConfiguration;
import org.eobjects.metamodel.util.FileResource;
import org.junit.Test;

public class FlatFileToolTest {

    FlatFileTool flatFileTool;

    @Test
    public void test() throws Exception {
        String[] args = new String[2];
        args[0] = "src/test/resources/countrycodes.csv";
        args[1] = "output";
        AnalyzerBeansConfiguration analyzerBeansConfiguration = buildAnalyzerBeansConfigurationLocalFS(args[0]);
        AnalysisJob analysisJob = buildAnalysisJob(analyzerBeansConfiguration, args[0]);
        flatFileTool = new FlatFileTool(analyzerBeansConfiguration, analysisJob);
        int exitCode = ToolRunner.run(flatFileTool, args);
        Assert.assertEquals(0, exitCode);
    }

    public static AnalyzerBeansConfiguration buildAnalyzerBeansConfigurationLocalFS(String csvFilePath) {
        CsvConfiguration csvConfiguration = new CsvConfiguration(1, "UTF8", ';', '"', '\\');
        Datastore datastore = new CsvDatastore(csvFilePath, new FileResource(csvFilePath), csvConfiguration);

        DatastoreCatalog datastoreCatalog = new DatastoreCatalogImpl(datastore);

        SimpleDescriptorProvider descriptorProvider = new SimpleDescriptorProvider(true);
        descriptorProvider.addTransformerBeanDescriptor(Descriptors.ofTransformer(ConcatenatorTransformer.class));
        descriptorProvider.addTransformerBeanDescriptor(Descriptors.ofTransformer(TokenizerTransformer.class));
        descriptorProvider.addAnalyzerBeanDescriptor(Descriptors.ofAnalyzer(InsertIntoTableAnalyzer.class));
        descriptorProvider.addAnalyzerBeanDescriptor(Descriptors.ofAnalyzer(StringAnalyzer.class));

        return new AnalyzerBeansConfigurationImpl().replace(datastoreCatalog).replace(descriptorProvider);
    }

    public static AnalysisJob buildAnalysisJob(AnalyzerBeansConfiguration configuration, String datastoreName) {
        AnalysisJobBuilder ajb = new AnalysisJobBuilder(configuration);
        try {
            ajb.setDatastore(datastoreName);
            ajb.addSourceColumns("countrycodes.csv.countrycodes.Country name",
                    "countrycodes.csv.countrycodes.ISO 3166-2", "countrycodes.csv.countrycodes.ISO 3166-3",
                    "countrycodes.csv.countrycodes.Synonym3");

            TransformerJobBuilder<ConcatenatorTransformer> concatenator = ajb
                    .addTransformer(ConcatenatorTransformer.class);
            concatenator.addInputColumns(ajb.getSourceColumnByName("countrycodes.csv.countrycodes.ISO 3166-2"));
            concatenator.addInputColumns(ajb.getSourceColumnByName("countrycodes.csv.countrycodes.ISO 3166-3"));
            concatenator.setConfiguredProperty("Separator", "_");

            AnalyzerJobBuilder<ValueDistributionAnalyzer> valueDistributionAnalyzer = ajb
                    .addAnalyzer(ValueDistributionAnalyzer.class);
            valueDistributionAnalyzer.addInputColumn(ajb
                    .getSourceColumnByName("countrycodes.csv.countrycodes.Country name"));

            AnalyzerJobBuilder<ValueDistributionAnalyzer> valueDistributionAnalyzer2 = ajb
                    .addAnalyzer(ValueDistributionAnalyzer.class);
            valueDistributionAnalyzer2.addInputColumn(ajb
                    .getSourceColumnByName("countrycodes.csv.countrycodes.ISO 3166-2"));

            return ajb.toAnalysisJob();
        } finally {
            ajb.close();
        }
    }

}
