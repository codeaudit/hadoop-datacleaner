package org.eobjects.hadoopdatacleaner.configuration.sample;

import java.util.ArrayList;
import java.util.List;

import org.eobjects.analyzer.beans.StringAnalyzer;
import org.eobjects.analyzer.beans.transform.ConcatenatorTransformer;
import org.eobjects.analyzer.beans.transform.TokenizerTransformer;
import org.eobjects.analyzer.beans.valuedist.ValueDistributionAnalyzer;
import org.eobjects.analyzer.beans.writers.InsertIntoTableAnalyzer;
import org.eobjects.analyzer.configuration.AnalyzerBeansConfiguration;
import org.eobjects.analyzer.configuration.AnalyzerBeansConfigurationImpl;
import org.eobjects.analyzer.connection.Datastore;
import org.eobjects.analyzer.connection.DatastoreCatalog;
import org.eobjects.analyzer.connection.DatastoreCatalogImpl;
import org.eobjects.analyzer.connection.PojoDatastore;
import org.eobjects.analyzer.descriptors.Descriptors;
import org.eobjects.analyzer.descriptors.SimpleDescriptorProvider;
import org.eobjects.analyzer.job.AnalysisJob;
import org.eobjects.analyzer.job.builder.AnalysisJobBuilder;
import org.eobjects.analyzer.job.builder.AnalyzerJobBuilder;
import org.eobjects.analyzer.job.builder.TransformerJobBuilder;
import org.eobjects.metamodel.pojo.ArrayTableDataProvider;
import org.eobjects.metamodel.pojo.TableDataProvider;
import org.eobjects.metamodel.util.SimpleTableDef;

public class SampleHBaseConfiguration {

    public static AnalyzerBeansConfiguration buildAnalyzerBeansConfiguration() {
        List<TableDataProvider<?>> tableDataProviders = new ArrayList<TableDataProvider<?>>();
        SimpleTableDef tableDef1 = new SimpleTableDef("countrycodes", new String[] {"mainFamily:country_name", "mainFamily:iso2", "mainFamily:iso3"});
        SimpleTableDef tableDef2 = new SimpleTableDef("countrycodes_output", new String[] {"mainFamily:country_name", "mainFamily:iso2", "mainFamily:iso3"});
        tableDataProviders.add(new ArrayTableDataProvider(tableDef1, new ArrayList<Object[]>()));
        tableDataProviders.add(new ArrayTableDataProvider(tableDef2, new ArrayList<Object[]>()));
        Datastore datastore = new PojoDatastore("countrycodes_hbase", "countrycodes_schema", tableDataProviders);
        
        DatastoreCatalog datastoreCatalog = new DatastoreCatalogImpl(datastore);

        SimpleDescriptorProvider descriptorProvider = new SimpleDescriptorProvider(
                true);
        descriptorProvider.addTransformerBeanDescriptor(Descriptors
                .ofTransformer(ConcatenatorTransformer.class));
        descriptorProvider.addTransformerBeanDescriptor(Descriptors
                .ofTransformer(TokenizerTransformer.class));
        descriptorProvider.addAnalyzerBeanDescriptor(Descriptors
                .ofAnalyzer(InsertIntoTableAnalyzer.class));
        descriptorProvider.addAnalyzerBeanDescriptor(Descriptors
                .ofAnalyzer(StringAnalyzer.class));

        return new AnalyzerBeansConfigurationImpl().replace(datastoreCatalog)
                .replace(descriptorProvider);
    }

    public static AnalysisJob buildAnalysisJob(
            AnalyzerBeansConfiguration configuration) {
        AnalysisJobBuilder ajb = new AnalysisJobBuilder(configuration);
        try {
            ajb.setDatastore("countrycodes_hbase");
            
            ajb.addSourceColumns("countrycodes.mainFamily:country_name",
                    "countrycodes.mainFamily:iso2",
                    "countrycodes.mainFamily:iso3");

            TransformerJobBuilder<ConcatenatorTransformer> concatenator = ajb
                    .addTransformer(ConcatenatorTransformer.class);
            concatenator.addInputColumns(ajb.getSourceColumnByName("mainFamily:iso2"));
            concatenator.addInputColumns(ajb.getSourceColumnByName("mainFamily:iso3"));
            concatenator.setConfiguredProperty("Separator", "_");
            
//          TransformerJobBuilder<TokenizerTransformer> tokenizer = ajb.addTransformer(TokenizerTransformer.class);
//          tokenizer.setConfiguredProperty("Token target", TokenizerTransformer.TokenTarget.COLUMNS);
//          tokenizer.addInputColumns(concatenator.getOutputColumns().get(0));
//          tokenizer.setConfiguredProperty("Number of tokens", 2);
//          tokenizer.setConfiguredProperty("Delimiters", new char[] { '_' });
//          tokenizer.getOutputColumns().get(0).setName("tokenized");
            
            AnalyzerJobBuilder<ValueDistributionAnalyzer> valueDistributionAnalyzer = ajb.addAnalyzer(ValueDistributionAnalyzer.class);
            valueDistributionAnalyzer.addInputColumn(ajb.getSourceColumnByName("mainFamily:country_name"));

            return ajb.toAnalysisJob();
        } finally {
            ajb.close();
        }
    }
    
}