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
		while (true){
			int val = in.readBits(BITS_PER_WORD);
			if (val == -1) break;
			out.writeBits(BITS_PER_WORD, val);
		}
		out.close();
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
		if(cod_check != HUFF_TREE){throw new HuffException("invalid magic number "+bits);}

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
				if (bits == 0) current = current.left; 		// read a 0, go left
				else current = current.right;				// read a 1, go right

				// if we reach a leaf node decode the character
				if (current.left == null && current.right == null) {
					// if decoded character is pseudo_eof stop decompressing
					if (current.value == PSEUDO_EOF)
						break;   // out of loop
					else {	// otherwise write decoded character 'BITS_PER_WORD' total bits to the output file 'out'
						out.writeBits(BITS_PER_WORD, current.value);
						current = root; // start back at the root for nect character decoding
					}
				}
			}
		}

		// Close the output file
		out.close();
	}

}