#include <iostream>
#include <vector>
#include <string>
#include <cstring>
#include <cmath>
#include <algorithm>
#include <memory>
#include <fstream>
#include <stdexcept>

// Comment these out if you want to use actual STB libraries
// For this demo, we'll implement a simple BMP reader/writer
#define DEMO_MODE

#ifdef DEMO_MODE
// Simple BMP handling for demonstration
#pragma pack(push, 1)
struct BMPFileHeader {
    uint16_t fileType{0x4D42};  // "BM"
    uint32_t fileSize{0};
    uint16_t reserved1{0};
    uint16_t reserved2{0};
    uint32_t offsetData{54};
};

struct BMPInfoHeader {
    uint32_t size{40};
    int32_t width{0};
    int32_t height{0};
    uint16_t planes{1};
    uint16_t bitCount{24};
    uint32_t compression{0};
    uint32_t sizeImage{0};
    int32_t xPixelsPerMeter{0};
    int32_t yPixelsPerMeter{0};
    uint32_t colorsUsed{0};
    uint32_t colorsImportant{0};
};
#pragma pack(pop)

#else
// Use actual STB libraries
#define STB_IMAGE_IMPLEMENTATION
#include "stb_image.h"
#define STB_IMAGE_WRITE_IMPLEMENTATION
#include "stb_image_write.h"
#endif

class Image {
private:
    int width;
    int height;
    int channels;
    std::vector<unsigned char> data;

public:
    Image() : width(0), height(0), channels(0) {}
    
    Image(int w, int h, int c) : width(w), height(h), channels(c) {
        data.resize(w * h * c);
    }
    
    // Copy constructor
    Image(const Image& other) 
        : width(other.width), height(other.height), channels(other.channels), data(other.data) {}
    
    // Assignment operator
    Image& operator=(const Image& other) {
        if (this != &other) {
            width = other.width;
            height = other.height;
            channels = other.channels;
            data = other.data;
        }
        return *this;
    }
    
    // Getters
    int getWidth() const { return width; }
    int getHeight() const { return height; }
    int getChannels() const { return channels; }
    
    // Get pixel value
    unsigned char getPixel(int x, int y, int c) const {
        if (x < 0 || x >= width || y < 0 || y >= height || c < 0 || c >= channels) {
            throw std::out_of_range("Pixel coordinates out of range");
        }
        return data[(y * width + x) * channels + c];
    }
    
    // Set pixel value
    void setPixel(int x, int y, int c, unsigned char value) {
        if (x < 0 || x >= width || y < 0 || y >= height || c < 0 || c >= channels) {
            throw std::out_of_range("Pixel coordinates out of range");
        }
        data[(y * width + x) * channels + c] = value;
    }
    
#ifdef DEMO_MODE
    // Simple BMP loading
    bool load(const std::string& filename) {
        std::ifstream file(filename, std::ios::binary);
        if (!file) {
            std::cerr << "Error: Cannot open file " << filename << std::endl;
            return false;
        }
        
        BMPFileHeader fileHeader;
        BMPInfoHeader infoHeader;
        
        file.read(reinterpret_cast<char*>(&fileHeader), sizeof(fileHeader));
        file.read(reinterpret_cast<char*>(&infoHeader), sizeof(infoHeader));
        
        if (fileHeader.fileType != 0x4D42) {
            std::cerr << "Error: Not a BMP file" << std::endl;
            return false;
        }
        
        width = infoHeader.width;
        height = std::abs(infoHeader.height);
        channels = infoHeader.bitCount / 8;
        
        if (channels != 3 && channels != 4) {
            std::cerr << "Error: Only 24-bit and 32-bit BMP supported" << std::endl;
            return false;
        }
        
        data.resize(width * height * channels);
        
        // BMP files are stored bottom-to-top
        int rowSize = ((width * channels + 3) / 4) * 4; // Row size must be multiple of 4
        std::vector<unsigned char> rowData(rowSize);
        
        file.seekg(fileHeader.offsetData, std::ios::beg);
        
        for (int y = height - 1; y >= 0; y--) {
            file.read(reinterpret_cast<char*>(rowData.data()), rowSize);
            for (int x = 0; x < width; x++) {
                for (int c = 0; c < channels; c++) {
                    // BMP stores in BGR order
                    int bmpC = (channels == 3) ? (2 - c) : c;
                    data[(y * width + x) * channels + c] = rowData[x * channels + bmpC];
                }
            }
        }
        
        std::cout << "Loaded BMP: " << filename << " (" << width << "x" << height << ")" << std::endl;
        return true;
    }
    
    // Simple BMP saving
    bool save(const std::string& filename) {
        if (channels != 3) {
            std::cerr << "Error: Only RGB images can be saved as BMP" << std::endl;
            return false;
        }
        
        std::ofstream file(filename, std::ios::binary);
        if (!file) {
            std::cerr << "Error: Cannot create file " << filename << std::endl;
            return false;
        }
        
        BMPFileHeader fileHeader;
        BMPInfoHeader infoHeader;
        
        int rowSize = ((width * 3 + 3) / 4) * 4;
        
        fileHeader.fileSize = sizeof(BMPFileHeader) + sizeof(BMPInfoHeader) + rowSize * height;
        
        infoHeader.width = width;
        infoHeader.height = height;
        infoHeader.sizeImage = rowSize * height;
        
        file.write(reinterpret_cast<char*>(&fileHeader), sizeof(fileHeader));
        file.write(reinterpret_cast<char*>(&infoHeader), sizeof(infoHeader));
        
        std::vector<unsigned char> rowData(rowSize, 0);
        
        for (int y = height - 1; y >= 0; y--) {
            for (int x = 0; x < width; x++) {
                // Convert RGB to BGR
                rowData[x * 3 + 0] = data[(y * width + x) * 3 + 2]; // B
                rowData[x * 3 + 1] = data[(y * width + x) * 3 + 1]; // G
                rowData[x * 3 + 2] = data[(y * width + x) * 3 + 0]; // R
            }
            file.write(reinterpret_cast<char*>(rowData.data()), rowSize);
        }
        
        std::cout << "Saved BMP: " << filename << " (" << width << "x" << height << ")" << std::endl;
        return true;
    }
#else
    // STB-based loading
    bool load(const std::string& filename) {
        int w, h, c;
        unsigned char* imgData = stbi_load(filename.c_str(), &w, &h, &c, 0);
        
        if (!imgData) {
            std::cerr << "Error loading image: " << filename << std::endl;
            return false;
        }
        
        width = w;
        height = h;
        channels = c;
        data.assign(imgData, imgData + (w * h * c));
        
        stbi_image_free(imgData);
        std::cout << "Loaded: " << filename << " (" << width << "x" << height << ", " << channels << " channels)" << std::endl;
        return true;
    }
    
    // STB-based saving
    bool save(const std::string& filename) {
        std::string ext = filename.substr(filename.find_last_of(".") + 1);
        bool result = false;
        
        if (ext == "png") {
            result = stbi_write_png(filename.c_str(), width, height, channels, data.data(), width * channels);
        } else if (ext == "jpg" || ext == "jpeg") {
            result = stbi_write_jpg(filename.c_str(), width, height, channels, data.data(), 90);
        } else if (ext == "bmp") {
            result = stbi_write_bmp(filename.c_str(), width, height, channels, data.data());
        }
        
        if (result) {
            std::cout << "Saved: " << filename << std::endl;
        } else {
            std::cerr << "Error saving image: " << filename << std::endl;
        }
        
        return result;
    }
#endif
    
    // Convert to grayscale
    void toGrayscale() {
        if (channels == 1) {
            std::cout << "Image is already grayscale" << std::endl;
            return;
        }
        
        std::vector<unsigned char> grayData(width * height);
        
        for (int i = 0; i < width * height; i++) {
            int srcIndex = i * channels;
            float gray = 0.299f * data[srcIndex] + 
                        0.587f * data[srcIndex + 1] + 
                        0.114f * data[srcIndex + 2];
            grayData[i] = static_cast<unsigned char>(std::min(255.0f, gray));
        }
        
        data = grayData;
        channels = 1;
        std::cout << "Converted to grayscale" << std::endl;
    }
    
    // Resize using bilinear interpolation
    void resize(int newWidth, int newHeight) {
        if (newWidth <= 0 || newHeight <= 0) {
            std::cerr << "Invalid dimensions" << std::endl;
            return;
        }
        
        std::vector<unsigned char> newData(newWidth * newHeight * channels);
        
        float xScale = static_cast<float>(width - 1) / (newWidth - 1);
        float yScale = static_cast<float>(height - 1) / (newHeight - 1);
        
        for (int y = 0; y < newHeight; y++) {
            for (int x = 0; x < newWidth; x++) {
                float srcX = x * xScale;
                float srcY = y * yScale;
                
                int x0 = static_cast<int>(srcX);
                int y0 = static_cast<int>(srcY);
                int x1 = std::min(x0 + 1, width - 1);
                int y1 = std::min(y0 + 1, height - 1);
                
                float fx = srcX - x0;
                float fy = srcY - y0;
                
                for (int c = 0; c < channels; c++) {
                    float p00 = getPixel(x0, y0, c);
                    float p10 = getPixel(x1, y0, c);
                    float p01 = getPixel(x0, y1, c);
                    float p11 = getPixel(x1, y1, c);
                    
                    float value = (1 - fx) * (1 - fy) * p00 +
                                 fx * (1 - fy) * p10 +
                                 (1 - fx) * fy * p01 +
                                 fx * fy * p11;
                    
                    newData[(y * newWidth + x) * channels + c] = static_cast<unsigned char>(value);
                }
            }
        }
        
        data = newData;
        width = newWidth;
        height = newHeight;
        std::cout << "Resized to " << width << "x" << height << std::endl;
    }
    
    // Apply Gaussian blur
    void gaussianBlur(float sigma) {
        if (sigma <= 0) return;
        
        int radius = static_cast<int>(ceil(3 * sigma));
        int kernelSize = 2 * radius + 1;
        
        // Generate Gaussian kernel
        std::vector<float> kernel(kernelSize);
        float sum = 0;
        
        for (int i = 0; i < kernelSize; i++) {
            int x = i - radius;
            kernel[i] = exp(-(x * x) / (2 * sigma * sigma));
            sum += kernel[i];
        }
        
        // Normalize kernel
        for (float& k : kernel) {
            k /= sum;
        }
        
        // Apply separable filter (horizontal then vertical)
        std::vector<unsigned char> temp(data);
        
        // Horizontal pass
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                for (int c = 0; c < channels; c++) {
                    float value = 0;
                    for (int i = 0; i < kernelSize; i++) {
                        int srcX = std::clamp(x + i - radius, 0, width - 1);
                        value += kernel[i] * data[(y * width + srcX) * channels + c];
                    }
                    temp[(y * width + x) * channels + c] = static_cast<unsigned char>(value);
                }
            }
        }
        
        // Vertical pass
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                for (int c = 0; c < channels; c++) {
                    float value = 0;
                    for (int i = 0; i < kernelSize; i++) {
                        int srcY = std::clamp(y + i - radius, 0, height - 1);
                        value += kernel[i] * temp[(srcY * width + x) * channels + c];
                    }
                    data[(y * width + x) * channels + c] = static_cast<unsigned char>(value);
                }
            }
        }
        
        std::cout << "Applied Gaussian blur (sigma=" << sigma << ")" << std::endl;
    }
    
    // Edge detection using Sobel operator
    void edgeDetection() {
        if (channels != 1) {
            toGrayscale();
        }
        
        std::vector<unsigned char> edges(width * height);
        
        int sobelX[3][3] = {{-1, 0, 1}, {-2, 0, 2}, {-1, 0, 1}};
        int sobelY[3][3] = {{-1, -2, -1}, {0, 0, 0}, {1, 2, 1}};
        
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                int gx = 0, gy = 0;
                
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        int pixel = getPixel(x + dx, y + dy, 0);
                        gx += pixel * sobelX[dy + 1][dx + 1];
                        gy += pixel * sobelY[dy + 1][dx + 1];
                    }
                }
                
                int magnitude = static_cast<int>(sqrt(gx * gx + gy * gy));
                edges[y * width + x] = static_cast<unsigned char>(std::min(255, magnitude));
            }
        }
        
        data = edges;
        std::cout << "Applied edge detection" << std::endl;
    }
    
    // Histogram equalization
    void histogramEqualization() {
        if (channels != 1) {
            std::cerr << "Histogram equalization only works on grayscale images" << std::endl;
            return;
        }
        
        // Calculate histogram
        std::vector<int> histogram(256, 0);
        for (unsigned char pixel : data) {
            histogram[pixel]++;
        }
        
        // Calculate cumulative distribution
        std::vector<int> cdf(256);
        cdf[0] = histogram[0];
        for (int i = 1; i < 256; i++) {
            cdf[i] = cdf[i - 1] + histogram[i];
        }
        
        // Find minimum non-zero value
        int cdfMin = 0;
        for (int i = 0; i < 256; i++) {
            if (cdf[i] != 0) {
                cdfMin = cdf[i];
                break;
            }
        }
        
        // Apply equalization
        int totalPixels = width * height;
        for (size_t i = 0; i < data.size(); i++) {
            int value = data[i];
            data[i] = static_cast<unsigned char>(
                round((cdf[value] - cdfMin) * 255.0 / (totalPixels - cdfMin))
            );
        }
        
        std::cout << "Applied histogram equalization" << std::endl;
    }
    
    // Create test image
    static Image createTestImage(int width, int height) {
        Image img(width, height, 3);
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // Create a colorful gradient pattern
                img.setPixel(x, y, 0, (x * 255) / width);           // R
                img.setPixel(x, y, 1, (y * 255) / height);          // G
                img.setPixel(x, y, 2, ((x + y) * 255) / (width + height)); // B
            }
        }
        
        return img;
    }
    
    void printInfo() const {
        std::cout << "Image: " << width << "x" << height 
                  << ", " << channels << " channel(s)" << std::endl;
    }
};

// Main program
int main(int argc, char* argv[]) {
    std::cout << "=== Advanced C++ Image Processor ===" << std::endl;
    std::cout << "Image processing with multiple algorithms" << std::endl << std::endl;
    
    if (argc < 2) {
        std::cout << "Usage: " << argv[0] << " <command> [options]" << std::endl;
        std::cout << "\nCommands:" << std::endl;
        std::cout << "  test              - Create and process a test image" << std::endl;
        std::cout << "  process <input> <output> [operations]" << std::endl;
        std::cout << "\nOperations:" << std::endl;
        std::cout << "  -gray             - Convert to grayscale" << std::endl;
        std::cout << "  -resize WxH       - Resize to WxH" << std::endl;
        std::cout << "  -blur <sigma>     - Gaussian blur" << std::endl;
        std::cout << "  -edge             - Edge detection" << std::endl;
        std::cout << "  -equalize         - Histogram equalization" << std::endl;
        return 1;
    }
    
    std::string command = argv[1];
    
    if (command == "test") {
        std::cout << "Creating test image..." << std::endl;
        Image img = Image::createTestImage(256, 256);
        img.save("test_original.bmp");
        
        // Test various operations
        Image gray = img;
        gray.toGrayscale();
        gray.save("test_gray.bmp");
        
        Image resized = img;
        resized.resize(128, 128);
        resized.save("test_resized.bmp");
        
        Image blurred = img;
        blurred.gaussianBlur(2.0);
        blurred.save("test_blurred.bmp");
        
        Image edges = img;
        edges.edgeDetection();
        edges.save("test_edges.bmp");
        
        std::cout << "\nTest complete! Check the output files." << std::endl;
    }
    else if (command == "process" && argc >= 4) {
        std::string inputFile = argv[2];
        std::string outputFile = argv[3];
        
        Image img;
        if (!img.load(inputFile)) {
            return 1;
        }
        
        img.printInfo();
        
        // Process operations
        for (int i = 4; i < argc; i++) {
            std::string op = argv[i];
            
            if (op == "-gray") {
                img.toGrayscale();
            }
            else if (op == "-resize" && i + 1 < argc) {
                std::string size = argv[++i];
                size_t xPos = size.find('x');
                if (xPos != std::string::npos) {
                    int w = std::stoi(size.substr(0, xPos));
                    int h = std::stoi(size.substr(xPos + 1));
                    img.resize(w, h);
                }
            }
            else if (op == "-blur" && i + 1 < argc) {
                float sigma = std::stof(argv[++i]);
                img.gaussianBlur(sigma);
            }
            else if (op == "-edge") {
                img.edgeDetection();
            }
            else if (op == "-equalize") {
                if (img.getChannels() != 1) {
                    img.toGrayscale();
                }
                img.histogramEqualization();
            }
        }
        
        img.save(outputFile);
        img.printInfo();
    }
    else {
        std::cerr << "Invalid command or arguments" << std::endl;
        return 1;
    }
    
    return 0;
}