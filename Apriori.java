//This program finds the set of frequent pairs from an accompanying text file, retail.txt, by using Apriori algorithm.
//retail.txt contains ~88,000 lines of text comprised of numbers. Each line represents one "basket" while each number represents a product that has been mapped to an integer.

package pcy;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Apriori {
	public static void main(String [] args) throws NumberFormatException, IOException {
		long begin = System.currentTimeMillis();
		
		/*For this project, I was required to break down the input file into "chunks". I did this with percentages[]. The percentages represent the percentage of the input file we will read (for ex, 80% of the lines)
		 supports[] represents the support thresholds we are interested in, which are also expressed as a percentage. However in the code below I've commented out the list of supports & percentages. The code will run the algorithm once on the entire data set with a support threshold of 5%.  */
		
		//int[] supports = {1, 5, 10}; //array of support thresholds we are interested in, expressed as percentage of all pairs
		//int[] percentages = {1, 5, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100}; //percentage sizes of the data (chunk sizes)
		
		
		
		int[] supports = {5};
		int[] percentages = {100};
		BufferedReader input = new BufferedReader(new FileReader("retail.txt"));
		for(double sup : supports) {
			for(double per : percentages) {
				double num_baskets = 88162 * (per/100); //Notepad++ shows the file has 88162 lines/baskets. num_baskets is how many baskets we have to read for this chunk.
				int support_threshold = (int) ((88162*(per/100))*(sup/100)); //I got a little lazy here by hardcoding the total number of lines. I could have found it programmatically but it would require another read of the whole data, and isn't really the purpose of the exercise.
				System.out.println("Num baskets: "+num_baskets);
				System.out.println("Support threshold: "+support_threshold);
				
				//Create a hashmap to store <value, frequency> pairs.
				HashMap<Integer,Integer> hm = new HashMap<Integer,Integer>();
				//Get frequency of all singletons and store in hashmap
				findAllSingletons(hm, input, num_baskets);
				//prune non-frequent singletons 
				pruneSingletons(hm, support_threshold);
				System.out.println("frequent singletons: "+hm);
				
				//find candidate pairs by generating set of pairs from all combinations of frequent singletons
				int[][] pairs = new int[(hm.size()*(hm.size()-1)/2)][3]; //new table to store pairs, can't use a hashmap because there may be pairs with the same keys but we need to track them all. Inner tables store 3 values which represent {element1, element2, count}
				//find the pairs that can be considered candidates and add them to the pairs array
				pairs = findCandidatePairs(hm);
				//System.out.println(Arrays.deepToString(pairs));
				pairs = countCandidatePairs(hm, support_threshold, pairs);
				System.out.println("Candidate pairs and their frequencies: "+Arrays.deepToString(pairs));
				//find frequent pairs by counting the frequency of each pair
				ArrayList<Integer>[][] frequentpairs = findFrequentPairs(pairs, support_threshold);
				
				System.out.print("frequent pairs (count "+ frequentpairs.length +"): "+Arrays.deepToString(frequentpairs));
				System.out.println();
				
			}
		}
		
		long end = System.currentTimeMillis();
		long time = end-begin;
		System.out.println("Elapsed time: "+time+" ms");
	}
	public static void findAllSingletons(HashMap<Integer,Integer> hash, BufferedReader br, double num_baskets) throws NumberFormatException, IOException {
		String line;
		int num_baskets_read = 0;
		while((line=br.readLine()) != null && num_baskets_read < num_baskets) { //read the whole chunk one line at a time
			String[] linesplit = line.split(" ");
			for(int i = 0; i<linesplit.length; i++) {
				if(hash.containsKey(Integer.parseInt(linesplit[i]))) {
					hash.put(Integer.parseInt(linesplit[i]), hash.get(Integer.parseInt(linesplit[i]))+1); //increment value if key exists
				}
				else {
					hash.put(Integer.parseInt(linesplit[i]), 1); //add key to hashmap if it doesn't exist
				}
			}
			num_baskets_read++;
		}
	}
	
	//Gets rid of the non-frequent singletons. We do this because we know that frequent pairs will only be made up of frequent singletons, so the non-frequent ones are useless to us
	public static void pruneSingletons(HashMap<Integer,Integer> hash, int support_threshold) {
		for(Iterator<Map.Entry<Integer, Integer>> itr = hash.entrySet().iterator(); itr.hasNext(); ) { //Have to define an iterator because we can't modify the hashmap while iterating through it
			Map.Entry<Integer, Integer> entry = itr.next();
			if(entry.getValue() < support_threshold) {
	        	itr.remove();
	        }
		}
	}

	//generate the list of pairs from frequent singletons
	public static int[][] findCandidatePairs(HashMap<Integer,Integer> hashmap) {
		int[][] pairs = new int[hashmap.size()*(hashmap.size()-1)/2][3];
		int a = 0;
		int super_iterations = 0; //reset super_iterations
		int sub_iterations = 0;
		for(Iterator<Map.Entry<Integer, Integer>> itr = hashmap.entrySet().iterator(); itr.hasNext();) {
			Map.Entry<Integer, Integer> obj = itr.next(); //Calling .next() after the iterator has been declared sets the pointer to index 0
			if(!itr.hasNext()) { //Reached last element in the set so there are no more elements after this, so we're done.
				break;
			}
			for(Iterator<Map.Entry<Integer, Integer>> subitr = hashmap.entrySet().iterator(); subitr.hasNext();) {
				Map.Entry<Integer, Integer> subobj = subitr.next(); //set pointer to index 0
				
				if(a == 0 && subitr.hasNext()) { //If it's the first pair for a new n, then increment pointer by 1
					for(int j = 0; j<super_iterations; j++) { //Also increment pointer by 1 for each new n
						if(subitr.hasNext()) {
							subobj = subitr.next();
						}
					}
					if(subitr.hasNext()) {
						subobj = subitr.next();
					}
					a++;
				}
				
				pairs[sub_iterations][0] = obj.getKey();
				pairs[sub_iterations][1] = subobj.getKey();
				sub_iterations++;
			}
			a=0;
			super_iterations++;
		}
		return pairs;
	}
	
	//Find the frequency of the candidate pairs
	public static int[][] countCandidatePairs(HashMap<Integer,Integer> hashmap, int support_threshold, int[][]pairs) throws NumberFormatException, IOException {
		String line;
		BufferedReader input2 = new BufferedReader(new FileReader("retail.txt"));
		while((line=input2.readLine()) != null) {
			
			String[] linesplit = line.split(" ");
			int[] array = new int[linesplit.length];
			for(int i = 0; i <linesplit.length; i++) {
				array[i] = Integer.parseInt(linesplit[i]);
			}

			for(int j = 0; j<(hashmap.size()*(hashmap.size()-1)/2); j++) {
				boolean containsElement1 = false; //reset booleans 
				boolean containsElement2 = false;
				for(int i = 0; i<array.length; i++) {
					//check if the line contains both elements. Don't count arrays that are all zeroes, stop counting the group when the support threshold is reached
					if((pairs[j][0] != 0 && pairs[j][1] != 0 ) && pairs[j][2] != support_threshold && pairs[j][0] == array[i]) {
						containsElement1 = true;
					}
					if((pairs[j][0] != 0 && pairs[j][1] != 0 ) && pairs[j][2] != support_threshold && pairs[j][1] == array[i]) {
						containsElement2 = true;
					}
					if(containsElement1 == true && containsElement2 == true) {
						pairs[j][2]++;
						break; //break to make sure we don't keep counting elements while both booleans are true
						
					}
					
					if(array[i] > pairs[j][0] && array[i] > pairs[j][1]) { //since each line is already in ascending order we can break when we have passed both elements in the pair. Saves a nice chunk of time.
						break;
					}
				}
			}
		}
		input2.close();
		
		return pairs;
		
	}
	
	
	//Get the pairs that occur more frequently than the support threshold
	public static ArrayList<Integer>[][] findFrequentPairs(int[][] pairs, int support_threshold) throws NumberFormatException, IOException {
		int top = 0; //defining integers to act as index for the frequentpairs ArrayList
		int bottom = 0;
		
		for(int i = 0; i<pairs.length; i++) {
			if(pairs[i][2] >= support_threshold) {
				top++;
			}
		}
		
		ArrayList<Integer>[][] frequentpairs = new ArrayList[top][1];
        
		for(int i = 0; i<pairs.length; i++) {
			if(pairs[i][0] != 0 && pairs[i][2] >= support_threshold) {
				frequentpairs[bottom][0] = new ArrayList<Integer>();
				frequentpairs[bottom][0].add(pairs[i][0]);
				frequentpairs[bottom][0].add(pairs[i][1]);
				bottom++;
			}
		}
		return frequentpairs;
	}
}
