/*******************************************************************************
 * Copyright 2015
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package de.tudarmstadt.ukp.dkpro.core.mallet.topicmodel;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import cc.mallet.topics.ParallelTopicModel;
import cc.mallet.types.Alphabet;
import cc.mallet.types.IDSorter;

public class MalletTopicModelUtils
{
    private static final Log LOGGER = LogFactory.getLog(MalletTopicModelUtils.class);

    /**
     * Retrieve the top n topic words for each topic in the given model.
     * 
     * @param modelFile
     *            the model file
     * @param nWords
     *            the maximum number of words to retrieve
     * @param normalize
     *            normalize the word weights ?
     * 
     * @return a list of maps where each map represents a topic, mapping words to weights
     * @throws Exception
     */
    public static List<Map<String, Double>> getTopWords(File modelFile, int nWords,
            boolean normalize)
                throws Exception
    {
        LOGGER.info("Reading model file " + modelFile + "...");
        ParallelTopicModel model = ParallelTopicModel.read(modelFile);
        Alphabet alphabet = model.getAlphabet();

        List<Map<String, Double>> topics = new ArrayList<>(model.getNumTopics());

        /* iterate over topics */
        for (TreeSet<IDSorter> topic : model.getSortedWords()) {
            Map<String, Double> topicWords = new HashMap<>(nWords);

            /* iterate over word IDs in topic (sorted by weight) */
            for (IDSorter id : topic) {
                double weight = normalize ? id.getWeight() / alphabet.size() : id.getWeight(); // normalize
                String word = (String) alphabet.lookupObject(id.getID());

                topicWords.put(word, weight);

                if (topicWords.size() >= nWords) {
                    break; // go to next topic
                }
            }
            topics.add(topicWords);
        }
        return topics;
    }

    /**
     * Print the top n words of each topic into a file.
     * 
     * @param modelFile
     *            the model file
     * @param targetFile
     *            the file in which the topic words are written
     * @param nWords
     *            the number of words to extract
     * @throws Exception
     *             if the model file cannot be read or if the target file cannot be written
     */
    public static void printTopicWords(File modelFile, File targetFile, int nWords)
        throws Exception
    {
        boolean newLineAfterEachWord = false;

        ParallelTopicModel model = ParallelTopicModel.read(modelFile);
        model.printTopWords(targetFile, nWords, newLineAfterEachWord);
    }

}
