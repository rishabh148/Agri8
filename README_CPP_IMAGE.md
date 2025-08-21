# C++ Image Processing Solution

This repository contains two C++ image processing programs that demonstrate various image manipulation algorithms.

## Programs

### 1. image_processor.cpp
A simple image processor that demonstrates basic image operations:
- Image loading/saving (simulated)
- Grayscale conversion
- Image resizing (nearest neighbor)
- Box blur filtering
- Brightness adjustment
- Horizontal/vertical flipping

### 2. image_processor_advanced.cpp
An advanced image processor with more sophisticated algorithms:
- BMP file I/O (or STB library support)
- Grayscale conversion with proper luminance formula
- Bilinear interpolation for resizing
- Gaussian blur with separable filters
- Sobel edge detection
- Histogram equalization
- Test image generation

## Building

```bash
# Compile both programs
make

# Compile individual programs
make image_processor
make image_processor_advanced

# Run tests
make test

# Clean build files
make clean
```

## Usage

### Simple Image Processor
```bash
# Run demonstration
./image_processor demo

# Show help
./image_processor
```

### Advanced Image Processor
```bash
# Create and process test images
./image_processor_advanced test

# Process an image with multiple operations
./image_processor_advanced process input.bmp output.bmp -gray -resize 256x256 -blur 2.0

# Apply edge detection
./image_processor_advanced process input.bmp edges.bmp -edge

# Apply histogram equalization (grayscale only)
./image_processor_advanced process input.bmp equalized.bmp -gray -equalize
```

## Features

### Image Operations
1. **Grayscale Conversion**: Uses standard luminance formula (0.299R + 0.587G + 0.114B)
2. **Resize**: 
   - Simple version: Nearest neighbor interpolation
   - Advanced version: Bilinear interpolation
3. **Blur**:
   - Simple version: Box filter
   - Advanced version: Gaussian blur with configurable sigma
4. **Edge Detection**: Sobel operator for gradient magnitude
5. **Histogram Equalization**: Enhances contrast in grayscale images
6. **Brightness Adjustment**: Multiply pixel values by a factor
7. **Flip**: Horizontal and vertical image flipping

### File Formats
- Simple version: Simulated I/O (creates test patterns)
- Advanced version: BMP file support (24-bit RGB)
- Can be extended to support PNG/JPEG with STB libraries

## Algorithm Details

### Gaussian Blur
- Uses separable filters for efficiency
- Kernel size based on sigma (radius = 3 * sigma)
- Two-pass algorithm (horizontal then vertical)

### Edge Detection
- Sobel operator with 3x3 kernels
- Calculates gradient magnitude
- Automatically converts to grayscale first

### Histogram Equalization
- Calculates cumulative distribution function
- Redistributes pixel intensities
- Works only on grayscale images

## Extending the Code

To use actual image libraries:
1. Download STB libraries (stb_image.h, stb_image_write.h)
2. Comment out `#define DEMO_MODE` in image_processor_advanced.cpp
3. Recompile the program

## Performance Considerations

- Bilinear interpolation is slower but produces better results than nearest neighbor
- Gaussian blur uses separable filters (O(n*m*r) instead of O(n*m*r²))
- Edge detection requires grayscale conversion first
- Large images may require optimization for real-time processing

## Error Handling

Both programs include:
- File I/O error checking
- Dimension validation
- Channel count verification
- Out-of-bounds pixel access protection

## Example Output

Running `make test` will generate:
- test_original.bmp: Colorful gradient test image
- test_gray.bmp: Grayscale version
- test_resized.bmp: Resized to 128x128
- test_blurred.bmp: Gaussian blur applied
- test_edges.bmp: Edge detection result