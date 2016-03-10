/*******************************************************************************
 * Copyright 2016
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package de.tudarmstadt.ukp.dkpro.core.mallet.wordembeddings;

import de.tudarmstadt.ukp.dkpro.core.api.resources.CompressionMethod;
import de.tudarmstadt.ukp.dkpro.core.api.resources.CompressionUtils;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.io.text.TextReader;
import de.tudarmstadt.ukp.dkpro.core.testing.DkproTestContext;
import de.tudarmstadt.ukp.dkpro.core.tokit.BreakIteratorSegmenter;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.junit.Rule;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import static junit.framework.TestCase.assertEquals;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;
import static org.junit.Assert.assertTrue;

public class WordEmbeddingsEstimatorTest
{
    @Rule
    public DkproTestContext testContext = new DkproTestContext();

    @Test(timeout = 60000)
    public void test()
            throws UIMAException, IOException
    {
        File text = new File("src/test/resources/txt/*");
        File embeddingsFile = new File(testContext.getTestOutputFolder(), "dummy.vec");
        boolean singularTarget = true;
        int expectedLength = 699;
        int dimensions = 50;

        CollectionReaderDescription reader = createReaderDescription(TextReader.class,
                TextReader.PARAM_SOURCE_LOCATION, text,
                TextReader.PARAM_LANGUAGE, "en");
        AnalysisEngineDescription segmenter = createEngineDescription(BreakIteratorSegmenter.class);
        AnalysisEngineDescription embeddings = createEngineDescription(
                WordEmbeddingsEstimator.class,
                WordEmbeddingsEstimator.PARAM_TARGET_LOCATION, embeddingsFile,
                WordEmbeddingsEstimator.PARAM_SINGULAR_TARGET, singularTarget,
                WordEmbeddingsEstimator.PARAM_OVERWRITE, true,
                WordEmbeddingsEstimator.PARAM_NUM_THREADS, 1,
                WordEmbeddingsEstimator.PARAM_MODEL_ENTITY_TYPE, Sentence.class.getCanonicalName());
        SimplePipeline.runPipeline(reader, segmenter, embeddings);

        List<String> output = Files.readAllLines(embeddingsFile.toPath());
        assertEquals(expectedLength, output.size());
        output.stream()
                .map(line -> line.split(" "))
                /* each line should have 1 + <#dimensions> fields */
                .peek(line -> assertEquals(dimensions + 1, line.length))
                /* each value must be parsable to a double */
                .map(line -> Arrays.copyOfRange(line, 1, dimensions))
                .forEach(array -> Arrays.stream(array).forEach(Double::parseDouble));

        embeddingsFile.delete();
    }

    @Test(timeout = 60000)
    public void testCompressed()
            throws UIMAException, IOException
    {
        CompressionMethod compressionMethod = CompressionMethod.GZIP;
        File text = new File("src/test/resources/txt/*");

        File targetDir = testContext.getTestOutputFolder();
        File targetFile = new File(targetDir, "embeddings" + compressionMethod.getExtension());
        int expectedLength = 699;
        int dimensions = 50;

        CollectionReaderDescription reader = createReaderDescription(TextReader.class,
                TextReader.PARAM_SOURCE_LOCATION, text,
                TextReader.PARAM_LANGUAGE, "en");
        AnalysisEngineDescription segmenter = createEngineDescription(BreakIteratorSegmenter.class);
        AnalysisEngineDescription embeddings = createEngineDescription(
                WordEmbeddingsEstimator.class,
                WordEmbeddingsEstimator.PARAM_TARGET_LOCATION, targetDir,
                WordEmbeddingsEstimator.PARAM_MODEL_ENTITY_TYPE, Sentence.class.getCanonicalName(),
                WordEmbeddingsEstimator.PARAM_NUM_THREADS, 1,
                WordEmbeddingsEstimator.PARAM_COMPRESSION, compressionMethod);
        SimplePipeline.runPipeline(reader, segmenter, embeddings);

        BufferedReader bufferedReader = new BufferedReader(
                new InputStreamReader(CompressionUtils.getInputStream(
                        targetFile.getAbsolutePath(), Files.newInputStream(targetFile.toPath()))));
        String line;
        int lineCounter = 0;
        while ((line = bufferedReader.readLine()) != null) {
            lineCounter++;
            String[] fields = line.split(" ");
            assertEquals(dimensions + 1, fields.length);
            assertTrue(Arrays.stream(fields, 1, fields.length)
                    .mapToDouble(Double::parseDouble)
                    .allMatch(f -> 1 > f && -1 < f));
        }
        assertEquals(expectedLength, lineCounter);

        bufferedReader.close();
    }

    @Test(timeout = 60000)
    public void testCharacterEmbeddings()
            throws IOException, UIMAException
    {
        File text = new File("src/test/resources/txt/*");
        File embeddingsFile = new File(testContext.getTestOutputFolder(), "embeddings.vec");
        boolean singularTarget = true;
        boolean useCharacters = true;
        int expectedLength = 47;
        int dimensions = 50;

        CollectionReaderDescription reader = createReaderDescription(TextReader.class,
                TextReader.PARAM_SOURCE_LOCATION, text,
                TextReader.PARAM_LANGUAGE, "en");
        AnalysisEngineDescription segmenter = createEngineDescription(BreakIteratorSegmenter.class);
        AnalysisEngineDescription embeddings = createEngineDescription(
                WordEmbeddingsEstimator.class,
                WordEmbeddingsEstimator.PARAM_TARGET_LOCATION, embeddingsFile,
                WordEmbeddingsEstimator.PARAM_SINGULAR_TARGET, singularTarget,
                WordEmbeddingsEstimator.PARAM_USE_CHARACTERS, useCharacters,
                WordEmbeddingsEstimator.PARAM_EXAMPLE_WORD, "a",
                WordEmbeddingsEstimator.PARAM_NUM_THREADS, 1,
                WordEmbeddingsEstimator.PARAM_OVERWRITE, true);
        SimplePipeline.runPipeline(reader, segmenter, embeddings);

        List<String> output = Files.readAllLines(embeddingsFile.toPath());
        assertEquals(expectedLength, output.size());
        output.stream()
                .map(line -> line.split(" "))
                /* each line should have 1 + <#dimensions> fields */
                .peek(line -> assertEquals(dimensions + 1, line.length))
                /* each value must be parsable to a double */
                .map(line -> Arrays.copyOfRange(line, 1, dimensions))
                .forEach(array -> Arrays.stream(array).forEach(Double::parseDouble));
    }
}