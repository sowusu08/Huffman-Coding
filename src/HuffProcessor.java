import javax.sound.midi.Soundbank;
import java.util.PriorityQueue;

/**
 * Although this class has a history of several years,
 * it is starting from a blank-slate, new and clean implementation
 * as of Fall 2018.
 * <P>
 * Changes include relying solely on a tree for header information
 * and including debug and bits read/written information
 *
 * @author Ow	en Astrachan
 *
 * Revise
 */

public class HuffProcessor {

	private class HuffNode implements Comparable<HuffNode> {
		HuffNode left;
		HuffNode right;
		int value;
		int weight;

		public HuffNode(int val, int count) {
			value = val;
			weight = count;
		}
		public HuffNode(int val, int count, HuffNode ltree, HuffNode rtree) {
			value = val;
			weight = count;
			left = ltree;
			right = rtree;
		}

		public int compareTo(HuffNode o) {
			return weight - o.weight;
		}
	}

	public static final int BITS_PER_WORD = 8;
	public static final int BITS_PER_INT = 32;
	public static final int ALPH_SIZE = (1 << BITS_PER_WORD);
	public static final int PSEUDO_EOF = ALPH_SIZE;
	public static final int HUFF_NUMBER = 0xface8200;
	public static final int HUFF_TREE  = HUFF_NUMBER | 1;

	private boolean myDebugging = false;

	public HuffProcessor() {
		this(false);
	}

	public HuffProcessor(boolean debug) {
		myDebugging = debug;
	}

	/**
	 * Compresses a file. Process must be reversible and loss-less.
	 *
	 * @param in
	 *            Buffered bit stream of the file to be compressed.
	 * @param out
	 *            Buffered bit stream writing to the output file.
	 */
	public void compress(BitInputStream in, BitOutputStream out){


		// remove all this code when implementing compress
		/*while (true){
			int val = in.readBits(BITS_PER_WORD);
			if (val == -1) break;
			out.writeBits(BITS_PER_WORD, val);
		}*/
		//1. Determine the frequency of every eight-bit character/chunk in the file being compressed
		System.out.println("hi");
		int[] counts = getCounts(in);
		System.out.println("helllooo");
		HuffNode root = makeTree(counts);
		in.reset();
		out.writeBits(BITS_PER_INT,HUFF_TREE);
		writeTree(root,out);
		String[] encodings = new String[ALPH_SIZE+1];
		makeEncodings(root,"",encodings);

		out.close();
	}

	private int[] getCounts(BitInputStream in){
		// initialize an array filled with zeros
		int[] ret = new int[ALPH_SIZE];

		while(true) {
			// read 8-bit characters/chunks
			int val = in.readBits(BITS_PER_WORD);
			if (val == -1) {
				break;
			} else {
				// if there are bits to read increment the element at the index corresponding to the value by one
				ret[val] += 1;
			}
		}
		return ret;
	}

	private HuffNode makeTree(int[] counts){
		// Creating the nodes for the tree
		PriorityQueue<HuffNode> pq = new PriorityQueue<>();
		for(int k = 0; k < counts.length; k++){                // where counts is an array where each index corresponds to ASCII encoding for a character
			if(counts[k] >0){                                    // and the value at the index is the nuber of times the character occurs
				pq.add(new HuffNode(k, counts[k], null, null));
			}
		}

		pq.add(new HuffNode(PSEUDO_EOF,1,null,null)); // account for PSEUDO_EOF having a single occurrence

		// Creating tree from HuffNodes
		while(pq.size() > 1){
			HuffNode left = pq.remove();
			HuffNode right = pq.remove();

			pq.add(new HuffNode(0, left.weight + right.weight, left, right));
		}

		// the last value to be removed is the root
		// return tree
		HuffNode root = pq.remove();
		return root;
	}

	private void makeEncodings(HuffNode tree, String s, String[] encodings){
		// where 'encodings' is an empty Strign array list of size ALPH_SIZE + 1
		// traverse the tree
		// for each leaf (each letter) add a '0' for each left we take on the path to the leaf
		// add a '1' for each right we take

		/*String[] encodings = new String[ALPH_SIZE + 1];*/ 	// String[] encodings should be made prior to call to makeEncodings()
		//makeEncodings(tree,"",encodings);

		// base case: node has no children
		if((tree.right == null && tree.left == null)) {
			// add the path to the encodings
			encodings[tree.value] = s;
			System.out.println("here");
			return;
		}

		// otherwise call makeEncodings on tree.left and tree.right
		if(tree.right != null){makeEncodings(tree.right, s+"1", encodings);}
		if(tree.left != null){makeEncodings(tree.left, s+"0", encodings);}

	}

	/**
	 * Decompresses a file. Output file must be identical bit-by-bit to the
	 * original.
	 *
	 * @param in
	 *            Buffered bit stream of the file to be decompressed.
	 * @param out
	 *            Buffered bit stream writing to the output file.
	 */
	public void decompress(BitInputStream in, BitOutputStream out){
		// remove all code when implementing decompress

		/*while (true){
			int val = in.readBits(BITS_PER_WORD);
			if (val == -1) break;
			out.writeBits(BITS_PER_WORD, val);
		}*/
		// check that the file is Huffman-coded using the first 32-bit number; if it isn't throw an exception
		int cod_check = in.readBits(BITS_PER_INT);
		//if (cod_check == -1) throw new HuffException("illegal header starts with " + cod_check);
		if(cod_check != HUFF_TREE){throw new HuffException("invalid magic number "+cod_check);}

		// Read/create the tree used to decompress/compress using .readTree() helper method and store in root
		HuffNode root = readTree(in);
		HuffNode current = root;

		// read the whole input file of bits and decompress it, writing this to the output file
		// stop once we reach PSEUDO_EOF
		while(true){
			int bits = in.readBits(1);

			// throw a HuffException if reading bits ever fails, i.e., the readBits method returns -1
			if(bits == -1) {
				throw new HuffException("bad input, not PSEUDO_EOF");
			} else {
				if (bits == 0) {
					current = current.left;        // read a 0, go left
				}else{
					current = current.right;	// read a 1, go right
				}

				// if we reach a leaf node decode the character
				if (current.left == null && current.right == null) {
					// if decoded character is pseudo_eof stop decompressing
					if (current.value == PSEUDO_EOF) {
						break;   // out of loop
					} else {	// otherwise write decoded character 'BITS_PER_WORD' total bits to the output file 'out'
						out.writeBits(BITS_PER_WORD, current.value);
						current = root; // start back at the root for next character decoding
					}
				}
			}
		}

		// Close the output file
		out.close();
	}

	private HuffNode readTree(BitInputStream in) {
		int bit = in.readBits(1);
		if (bit == -1) throw new HuffException("bad input");
		if (bit == 0) {
			HuffNode left = readTree(in);
			HuffNode right = readTree(in);
			return new HuffNode(0,0,left,right);
		}
		else {
			int value = in.readBits(BITS_PER_WORD+1); //read BITS_PER_WORD+1 bits from input
			return new HuffNode(value,0,null,null);
		}
	}

	private void writeTree(HuffNode node, BitOutputStream out){
		if(node.left==null&&node.right==null){
			out.writeBits(1,1);
			out.writeBits(BITS_PER_WORD+1,node.value);
		} else {
			out.writeBits(1,0);
			writeTree(node.left,out);
			writeTree(node.right,out);
		}
	}


}