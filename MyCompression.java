// CSCI-576: Assg-2 
// Date: Mar 5, 2025
// Author: Ameya Deshmukh

// imports
import java.awt.BorderLayout;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import javax.swing.*;

// class to implement image compression algorithm via k-means clustering for gray scale and color images
public class MyCompression {

    private static final int WIDTH  = 352;
    private static final int HEIGHT = 288;

    // limit set for k-means iteration
    private static final int MAX_ITERS = 50;
    // setting a strict threshold for sum of squared codeword changes
    private static final double CONVERGENCE_EPS = 0.5;
    private static final boolean VERBOSE = false;

    public static void main(String[] args) 
    {
        if (args.length != 3) 
        {
            System.err.println("Usage: java MyCompressionUnified <filename> <M> <N>");
            System.err.println("  <filename>: .raw (grayscale) or .rgb (color), 352x288");
            System.err.println("  <M>: either 2, or a perfect square (4,9,16,...)");
            System.err.println("  <N>: # of codewords (power of 2).");
            
            return;
        }

        String filename = args[0];
        int M = Integer.parseInt(args[1]);
        int N = Integer.parseInt(args[2]);

        // 2 different models with whem, M=2: 2-pixel approach AND when M=perfect square: NxN block
        boolean isTwoPixelMode = (M == 2);
        int blockSize = (int)Math.round(Math.sqrt(M));
        boolean isPerfectSquare = (blockSize * blockSize == M);

        if (!isTwoPixelMode && !isPerfectSquare) 
        {
            System.err.println("ERROR: M must be 2 OR a perfect square (4,9,16,...)");
            return;
        }

        // step 1: check if grayscale OR color by file size
        File f = new File(filename);
        long fileLen = f.length();
        long grayLen = WIDTH * HEIGHT;
        long colorLen = WIDTH * HEIGHT * 3;
        boolean isColor;

        if (fileLen == grayLen) 
        {
            isColor = false;
        } 
        else if (fileLen == colorLen) 
        {
            isColor = true;
        } 
        else 
        {
            System.err.println("ERROR: file size mismatch. Must be 352x288 or 3*(352x288).");
            return;
        }

        // step 2 - reading the image
        if (!isColor) {
            // here grayscale
            int[][] imageGray = readGrayscale(filename);

            if (imageGray == null) 
            {
                System.err.println("Could not read grayscale data.");
                return;
            }

            // buildng the vectors
            List<int[]> vectors;

            if (isTwoPixelMode) 
            {
                // here when M=2
                vectors = buildVectorsGray2Pixel(imageGray, WIDTH, HEIGHT);
            } 
            else 
            {
                // here when M=perfect square
                vectors = buildVectorsGrayBlock(imageGray, blockSize, WIDTH, HEIGHT);
            }

            // the k-means codebook
            List<int[]> codebook = buildCodebook(vectors, N);

            // reconstructing
            int[][] recGray;

            if (isTwoPixelMode) 
            {
                recGray = reconstructGray2Pixel(imageGray, codebook, WIDTH, HEIGHT);
            } 
            else 
            {
                recGray = reconstructGrayBlock(imageGray, codebook, blockSize, WIDTH, HEIGHT);
            }

            // showing the images side by side
            BufferedImage combined = makeSideBySideGray(imageGray, recGray, WIDTH, HEIGHT);
            displayImage(combined, "Original vs. Compressed (Grayscale, M=" + M + ")");

        } 
        else 
        {
            // the color
            int[][][] imageColor = readColor(filename);

            if (imageColor == null) 
            {
                System.err.println("Could not read color data.");
                return;
            }

            // building the vectors
            List<int[]> vectors;

            if (isTwoPixelMode) 
            {
                // here when we have M=2 --> dimension=6
                vectors = buildVectorsColor2Pixel(imageColor, WIDTH, HEIGHT);
            } 
            else 
            {
                // here when we have M=perfect square --> block-based
                vectors = buildVectorsColorBlock(imageColor, blockSize, WIDTH, HEIGHT);
            }

            // codebook
            List<int[]> codebook = buildCodebook(vectors, N);

            // reconstruct
            int[][][] recColor;
            if (isTwoPixelMode) 
            {
                recColor = reconstructColor2Pixel(imageColor, codebook, WIDTH, HEIGHT);
            } 
            else 
            {
                recColor = reconstructColorBlock(imageColor, codebook, blockSize, WIDTH, HEIGHT);
            }

            // display
            BufferedImage combined = makeSideBySideColor(imageColor, recColor, WIDTH, HEIGHT);
            displayImage(combined, "Original vs. Compressed (Color, M=" + M + ")");
        }
    }

    // READING THE IMAGES
    private static int[][] readGrayscale(String filename) 
    {
        int[][] img = new int[HEIGHT][WIDTH];

        try (FileInputStream fis = new FileInputStream(filename)) 
        {
            byte[] buffer = new byte[WIDTH * HEIGHT];
            int bytesRead = fis.read(buffer);

            if (bytesRead < WIDTH*HEIGHT) 
            {
                return null;
            }

            int idx = 0;

            for (int y = 0; y < HEIGHT; y++) 
            {
                for (int x = 0; x < WIDTH; x++) 
                {
                    img[y][x] = (buffer[idx] & 0xFF);
                    idx++;
                }
            }
        } 
        catch (IOException e) 
        {
            e.printStackTrace();
            return null;
        }

        return img;
    }

    private static int[][][] readColor(String filename) 
    {
        int[][][] img = new int[HEIGHT][WIDTH][3];

        try (FileInputStream fis = new FileInputStream(filename)) 
        {
            byte[] buffer = new byte[WIDTH * HEIGHT * 3];
            int bytesRead = fis.read(buffer);

            if (bytesRead < WIDTH*HEIGHT*3) 
            {
                return null;
            }

            int frameSize = WIDTH * HEIGHT;

            for (int y = 0; y < HEIGHT; y++) 
            {
                for (int x = 0; x < WIDTH; x++) 
                {
                    int idx = y*WIDTH + x;
                    int r = buffer[idx] & 0xFF;
                    int g = buffer[idx + frameSize] & 0xFF;
                    int b = buffer[idx + 2*frameSize] & 0xFF;
                    img[y][x][0] = r;
                    img[y][x][1] = g;
                    img[y][x][2] = b;
                }
            }
        } 
        catch (IOException e) 
        {
            e.printStackTrace();
            return null;
        }

        return img;
    }

    // BUILDING VECTORS FOR M=2
    private static List<int[]> buildVectorsGray2Pixel(int[][] image, int w, int h) 
    {
        List<int[]> vectors = new ArrayList<>();

        for (int y = 0; y < h; y++) 
        {
            for (int x = 0; x < w-1; x += 2) 
            {
                int p1 = image[y][x];
                int p2 = image[y][x+1];

                vectors.add(new int[]{ p1, p2 });
            }
        }

        return vectors;
    }

    private static List<int[]> buildVectorsColor2Pixel(int[][][] image, int w, int h) 
    {
        List<int[]> vectors = new ArrayList<>();

        for (int y = 0; y < h; y++) 
        {
            for (int x = 0; x < w-1; x += 2) 
            {
                int R1 = image[y][x][0];
                int G1 = image[y][x][1];
                int B1 = image[y][x][2];
                int R2 = image[y][x+1][0];
                int G2 = image[y][x+1][1];
                int B2 = image[y][x+1][2];
                vectors.add(new int[]{ R1, G1, B1, R2, G2, B2 });
            }
        }

        return vectors;
    }

    // BUILDING VECTORS FOR M=PERFECT SQUARE
    private static List<int[]> buildVectorsGrayBlock(int[][] image, int blockSize, int w, int h) 
    {
        List<int[]> vectors = new ArrayList<>();

        for (int by = 0; by < h; by += blockSize) 
        {
            for (int bx = 0; bx < w; bx += blockSize) 
            {
                int[] block = new int[blockSize*blockSize];
                int idx = 0;

                for (int yy = 0; yy < blockSize; yy++) 
                {
                    int srcY = Math.min(by+yy, h-1);

                    for (int xx = 0; xx < blockSize; xx++) 
                    {
                        int srcX = Math.min(bx+xx, w-1);
                        block[idx++] = image[srcY][srcX];
                    }
                }

                vectors.add(block);
            }
        }

        return vectors;
    }

    private static List<int[]> buildVectorsColorBlock(int[][][] image, int blockSize, int w, int h) 
    {
        List<int[]> vectors = new ArrayList<>();

        for (int by = 0; by < h; by += blockSize) 
        {
            for (int bx = 0; bx < w; bx += blockSize) 
            {
                int[] block = new int[3 * blockSize * blockSize];
                int idx = 0;

                for (int yy = 0; yy < blockSize; yy++) 
                {
                    int srcY = Math.min(by+yy, h-1);

                    for (int xx = 0; xx < blockSize; xx++) 
                    {
                        int srcX = Math.min(bx+xx, w-1);

                        block[idx++] = image[srcY][srcX][0];
                        block[idx++] = image[srcY][srcX][1];
                        block[idx++] = image[srcY][srcX][2];
                    }
                }

                vectors.add(block);
            }
        }

        return vectors;
    }

    // RECONSTRUCTING FOR M=2
    private static int[][] reconstructGray2Pixel(int[][] orig, List<int[]> codebook, int w, int h) 
    {
        int[][] rec = new int[h][w];

        // copying here
        for (int y = 0; y < h; y++) 
        {
            for (int x = 0; x < w; x++) 
            {
                rec[y][x] = orig[y][x];
            }
        }

        for (int y = 0; y < h; y++) 
        {
            for (int x = 0; x < w-1; x += 2) 
            {
                int[] pair = new int[]{ orig[y][x], orig[y][x+1] };
                int bestIndex = findNearest(pair, codebook);
                int[] cw = codebook.get(bestIndex);

                rec[y][x]   = cw[0];
                rec[y][x+1] = cw[1];
            }
        }

        return rec;
    }

    private static int[][][] reconstructColor2Pixel(int[][][] orig, List<int[]> codebook, int w, int h) 
    {
        int[][][] rec = new int[h][w][3];
        
        // copying here
        for (int y = 0; y < h; y++) 
        {
            for (int x = 0; x < w; x++) 
            {
                rec[y][x][0] = orig[y][x][0];
                rec[y][x][1] = orig[y][x][1];
                rec[y][x][2] = orig[y][x][2];
            }
        }

        for (int y = 0; y < h; y++) 
        {
            for (int x = 0; x < w-1; x += 2) 
            {
                int[] pair = new int[]
                {
                    orig[y][x][0],
                    orig[y][x][1],
                    orig[y][x][2],
                    orig[y][x+1][0],
                    orig[y][x+1][1],
                    orig[y][x+1][2]
                };

                int bestIndex = findNearest(pair, codebook);
                int[] cw = codebook.get(bestIndex);

                rec[y][x][0]   = cw[0];
                rec[y][x][1]   = cw[1];
                rec[y][x][2]   = cw[2];
                rec[y][x+1][0] = cw[3];
                rec[y][x+1][1] = cw[4];
                rec[y][x+1][2] = cw[5];
            }
        }

        return rec;
    }

    // RECONSTRUCTION FOR M=PERFECT SQUARE
    private static int[][] reconstructGrayBlock(int[][] orig, List<int[]> codebook, int blockSize, int w, int h) 
    {
        int[][] rec = new int[h][w];

        for (int y = 0; y < h; y++) 
        {
            for (int x = 0; x < w; x++) 
            {
                rec[y][x] = orig[y][x];
            }
        }

        for (int by = 0; by < h; by += blockSize) 
        {
            for (int bx = 0; bx < w; bx += blockSize) 
            {
                int[] blockVec = new int[blockSize*blockSize];
                int idx = 0;

                for (int yy = 0; yy < blockSize; yy++) 
                {
                    int srcY = Math.min(by+yy, h-1);

                    for (int xx = 0; xx < blockSize; xx++) 
                    {
                        int srcX = Math.min(bx+xx, w-1);
                        blockVec[idx++] = orig[srcY][srcX];
                    }
                }

                int bestIndex = findNearest(blockVec, codebook);
                int[] cw = codebook.get(bestIndex);

                idx = 0;

                for (int yy = 0; yy < blockSize; yy++) 
                {
                    int dstY = Math.min(by+yy, h-1);

                    for (int xx = 0; xx < blockSize; xx++) 
                    {
                        int dstX = Math.min(bx+xx, w-1);
                        rec[dstY][dstX] = cw[idx++];
                    }
                }
            }
        }

        return rec;
    }

    private static int[][][] reconstructColorBlock(int[][][] orig, List<int[]> codebook, int blockSize, int w, int h) 
    {
        int[][][] rec = new int[h][w][3];

        for (int y = 0; y < h; y++) 
        {
            for (int x = 0; x < w; x++) 
            {
                rec[y][x][0] = orig[y][x][0];
                rec[y][x][1] = orig[y][x][1];
                rec[y][x][2] = orig[y][x][2];
            }
        }

        for (int by = 0; by < h; by += blockSize) 
        {
            for (int bx = 0; bx < w; bx += blockSize) 
            {
                int[] blockVec = new int[3*blockSize*blockSize];
                int idx = 0;

                for (int yy = 0; yy < blockSize; yy++) 
                {
                    int srcY = Math.min(by+yy, h-1);

                    for (int xx = 0; xx < blockSize; xx++) 
                    {
                        int srcX = Math.min(bx+xx, w-1);
                        blockVec[idx++] = orig[srcY][srcX][0];
                        blockVec[idx++] = orig[srcY][srcX][1];
                        blockVec[idx++] = orig[srcY][srcX][2];
                    }
                }

                int bestIndex = findNearest(blockVec, codebook);
                int[] cw = codebook.get(bestIndex);

                idx = 0;

                for (int yy = 0; yy < blockSize; yy++) 
                {
                    int dstY = Math.min(by+yy, h-1);

                    for (int xx = 0; xx < blockSize; xx++) 
                    {
                        int dstX = Math.min(bx+xx, w-1);

                        rec[dstY][dstX][0] = cw[idx++];
                        rec[dstY][dstX][1] = cw[idx++];
                        rec[dstY][dstX][2] = cw[idx++];
                    }
                }
            }
        }
        return rec;
    }

    // KMEANS + STRICT CONVERGENCE + EMPTY CLUSTER REINIT
    private static List<int[]> buildCodebook(List<int[]> allVectors, int N) 
    {
        if (allVectors.isEmpty()) 
        {
            System.err.println("No vectors found, returning empty codebook.");
            return Collections.emptyList();
        }

        // step 1 - kmeans++ initializing
        List<int[]> codebook = kmeansPlusPlusInit(allVectors, N);

        // step 2 - iterating
        for (int iter = 0; iter < MAX_ITERS; iter++) {
            // assigning
            List<List<int[]>> clusters = new ArrayList<>();

            for (int i = 0; i < N; i++) 
            {
                clusters.add(new ArrayList<>());
            }
            for (int[] v : allVectors) 
            {
                int bestIndex = findNearest(v, codebook);
                clusters.get(bestIndex).add(v);
            }

            // updating
            double sqChange = updateCodewords(codebook, clusters);

            if (VERBOSE) 
            {
                System.out.println("Iter=" + iter + " sqChange=" + sqChange);
            }
            if (sqChange < CONVERGENCE_EPS) 
            {
                if (VERBOSE) 
                {
                    System.out.println("Converged early, sqChange=" + sqChange);
                }

                break;
            }
        }

        return codebook;
    }

    private static List<int[]> kmeansPlusPlusInit(List<int[]> allVectors, int N) 
    {
        Random rand = new Random();
        List<int[]> codebook = new ArrayList<>(N);

        // picking the first codeword randomly
        int firstIdx = rand.nextInt(allVectors.size());
        codebook.add(allVectors.get(firstIdx).clone());

        while (codebook.size() < N) 
        {
            double[] distSq = new double[allVectors.size()];
            double sumDist = 0;

            for (int i = 0; i < allVectors.size(); i++) 
            {
                int[] vec = allVectors.get(i);
                double minDist = Double.MAX_VALUE;

                for (int[] cw : codebook) 
                {
                    double dist = squaredDistance(vec, cw);

                    if (dist < minDist) 
                    {
                        minDist = dist;
                    }
                }

                distSq[i] = minDist;
                sumDist += minDist;
            }
            if (sumDist == 0.0) 
            {
                while (codebook.size() < N) 
                {
                    codebook.add(allVectors.get(0).clone());
                }

                break;
            }
            double r = rand.nextDouble() * sumDist;
            double cumsum = 0.0;

            for (int i = 0; i < allVectors.size(); i++) 
            {
                cumsum += distSq[i];

                if (cumsum >= r) 
                {
                    codebook.add(allVectors.get(i).clone());
                    break;
                }
            }
        }
        return codebook;
    }

    private static double updateCodewords(List<int[]> codebook, List<List<int[]>> clusters) 
    {
        double totalChange = 0.0;
        int d = codebook.get(0).length;

        int[][] newCentroids = new int[codebook.size()][d];
        boolean[] isEmpty = new boolean[codebook.size()];

        // computing the normal averages
        for (int i = 0; i < codebook.size(); i++) 
        {
            List<int[]> cluster = clusters.get(i);

            if (cluster.isEmpty()) 
            {
                isEmpty[i] = true;
                continue;
            }

            long[] sums = new long[d];

            for (int[] v : cluster) 
            {
                for (int k = 0; k < d; k++) 
                {
                    sums[k] += v[k];
                }
            }

            for (int k = 0; k < d; k++) 
            {
                newCentroids[i][k] = (int)(sums[k] / cluster.size());
            }
        }

        // handling the empty clusters --> reinit
        for (int i = 0; i < codebook.size(); i++) 
        {
            if (isEmpty[i]) 
            {
                // find largest cluster
                int largestIdx = -1;
                int largestSize = 0;

                for (int j = 0; j < clusters.size(); j++) 
                {
                    int sz = clusters.get(j).size();

                    if (sz > largestSize) 
                    {
                        largestSize = sz;
                        largestIdx = j;
                    }
                }
                if (largestIdx < 0 || largestSize == 0) 
                {
                    // fallbacking
                    newCentroids[i] = codebook.get(0).clone();
                    continue;
                }

                int[] oldCentroid = codebook.get(largestIdx);
                List<int[]> bigCluster = clusters.get(largestIdx);
                double bestDist = -1.0;
                int[] farthestVec = null;

                for (int[] v : bigCluster) 
                {
                    double dist = squaredDistance(v, oldCentroid);
                    if (dist > bestDist) 
                    {
                        bestDist = dist;
                        farthestVec = v;
                    }
                }
                if (farthestVec == null) 
                {
                    newCentroids[i] = codebook.get(largestIdx).clone();
                } 
                else 
                {
                    for (int k = 0; k < d; k++) 
                    {
                        newCentroids[i][k] = (oldCentroid[k] + farthestVec[k]) / 2;
                    }
                }
            }
        }

        // measuring the changes
        for (int i = 0; i < codebook.size(); i++) 
        {
            double dist = squaredDistance(codebook.get(i), newCentroids[i]);
            totalChange += dist;
            codebook.set(i, newCentroids[i]);
        }

        return totalChange;
    }

    private static double squaredDistance(int[] a, int[] b) 
    {
        double sum = 0.0;
        for (int i = 0; i < a.length; i++) 
        {
            double diff = a[i] - b[i];
            sum += diff*diff;
        }

        return sum;
    }

    private static int findNearest(int[] vec, List<int[]> codebook) 
    {
        int bestIndex = 0;
        double bestDist = Double.MAX_VALUE;

        for (int i = 0; i < codebook.size(); i++) {

            double dist = squaredDistance(vec, codebook.get(i));

            if (dist < bestDist) 
            {
                bestDist = dist;
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    // DISPLAYING THE IMAGES SIDE BY SIDE
    private static BufferedImage makeSideBySideGray(int[][] orig, int[][] rec, int w, int h) 
    {
        BufferedImage out = new BufferedImage(w*2, h, BufferedImage.TYPE_INT_RGB);
        
        // left=orig
        for (int y = 0; y < h; y++) 
        {
            for (int x = 0; x < w; x++) 
            {
                int val = orig[y][x];
                int rgb = 0xFF000000 | (val<<16)|(val<<8)|val;
                out.setRGB(x, y, rgb);
            }
        }
        // right=rec
        for (int y = 0; y < h; y++) 
        {
            for (int x = 0; x < w; x++) 
            {
                int val = rec[y][x];
                int rgb = 0xFF000000 | (val<<16)|(val<<8)|val;
                out.setRGB(x + w, y, rgb);
            }
        }

        return out;
    }

    private static BufferedImage makeSideBySideColor(int[][][] orig, int[][][] rec, int w, int h) 
    {
        BufferedImage out = new BufferedImage(w*2, h, BufferedImage.TYPE_INT_RGB);

        // left=orig
        for (int y = 0; y < h; y++) 
        {
            for (int x = 0; x < w; x++) 
            {
                int r = orig[y][x][0];
                int g = orig[y][x][1];
                int b = orig[y][x][2];
                int rgb = 0xFF000000 | (r<<16)|(g<<8)|b;
                out.setRGB(x, y, rgb);
            }
        }
        // right=rec
        for (int y = 0; y < h; y++) 
        {
            for (int x = 0; x < w; x++) 
            {
                int r = rec[y][x][0];
                int g = rec[y][x][1];
                int b = rec[y][x][2];
                int rgb = 0xFF000000 | (r<<16)|(g<<8)|b;
                out.setRGB(x + w, y, rgb);
            }
        }
        return out;
    }

    private static void displayImage(BufferedImage img, String title) 
    {
        JFrame f = new JFrame(title);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JLabel lab = new JLabel(new ImageIcon(img));
        f.getContentPane().add(lab, BorderLayout.CENTER);
        f.pack();
        f.setVisible(true);
    }
}
