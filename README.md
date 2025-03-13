# K-ImageCompressor: Image Compression Using K-Means Clustering 

## 📌 Project Overview
This project implements an **image compression** algorithm using **K-Means clustering** for grayscale and color images. The implementation supports:
- **2-Pixel encoding (M=2)**
- **Block-based encoding (M=4, 9, 16, ...)**
- **Codebook generation with K-Means clustering**
- **Reconstruction and visualization of original vs. compressed images**

## 📂 Repository Structure
```
📁 K-ImageCompressor
│── 📜 README.md      # Project documentation
│── 📜 MyCompression.java  # Main Java implementation
│── 📜 image1.raw     # Sample grayscale image (352x288)
│── 📜 image1.rgb     # Sample color image (352x288x3)
│── 📜 image2.raw     # Sample grayscale image (352x288)
│── 📜 image2.rgb     # Sample color image (352x288x3)
│── 📜 image3.raw     # Sample grayscale image (352x288)
│── 📜 image3.rgb     # Sample color image (352x288x3)
│── 📜 image4.raw     # Sample grayscale image (352x288)
│── 📜 image4.rgb     # Sample color image (352x288x3)
```

## 🚀 Getting Started
### Prerequisites
Ensure you have:
- **Java 8+** installed
- A **352x288 grayscale (.raw) or color (.rgb) image**

### Compilation & Execution
1. **Compile the Java file:**
   ```sh
   javac MyCompression.java
   ```
2. **Run the program with test images:**
   ```sh
   java MyCompression image1.raw 2 16
   java MyCompression image1.rgb 4 32
   java MyCompression image2.raw 2 8
   java MyCompression image2.rgb 4 16
   java MyCompression image3.raw 2 4
   java MyCompression image3.rgb 4 16
   java MyCompression image4.raw 2 32
   java MyCompression image4.rgb 4 64
   ```
   **Arguments:**
   - `<filename>`: Path to `.raw` (grayscale) or `.rgb` (color) image (352x288 resolution).
   - `<M>`: Encoding mode (either `2` or a perfect square like `4, 9, 16, ...`).
   - `<N>`: Number of codewords (power of 2, e.g., `2, 4, 8, 16, ...`).

### Example Usage
#### Grayscale Compression (2-Pixel Encoding)
```sh
java MyCompression image3.raw 2 16
```
#### Color Compression (Block-Based, 4x4)
```sh
java MyCompression image3.rgb 4 16
```

## 🎯 Features
- **Supports both grayscale and color images**
- **Adaptive vector quantization using K-Means++ initialization**
- **Strict convergence criteria for improved accuracy**
- **Side-by-side visualization of original and compressed images**
- **Handles empty clusters via reinitialization**

## 🖼️ Output
The program displays a side-by-side comparison of the **original vs. compressed** images using Java Swing.

## 📖 How It Works
1. **Reads the input image** (grayscale or color).
2. **Extracts pixel vectors** based on the chosen mode (`M`).
3. **Performs K-Means clustering** to generate a codebook (`N` codewords).
4. **Reconstructs the compressed image** using the nearest codeword.
5. **Displays the original and compressed images side by side.**

## 🛠️ Implementation Details
- **Vector Formation:**
  - For **M=2**: Uses **2 adjacent pixels** as a feature vector.
  - For **M is a square (4, 9, 16, ...)**: Uses **NxN blocks** as feature vectors.
- **K-Means++ Initialization:**
  - Ensures better clustering convergence.
- **Codebook Update:**
  - Uses squared error minimization.
- **Handling Empty Clusters:**
  - Reinitialize using the farthest vector from the largest cluster.

## 📌 Dependencies
- **Java Standard Library** (AWT, Swing, I/O)

## 📜 License
This project is licensed under the **MIT License**.

## 📬 Contact
For any queries, feel free to reach out!
- **Author:** Ameya Deshmukh
- **Email:** [ameyaudeshmukh@gmail.com](mailto:ameyaudeshmukh@gmail.com)

---
🎯 **If you like this project, don't forget to star ⭐ the repository!**
