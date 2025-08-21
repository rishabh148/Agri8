#include <iostream>
#include <vector>
#include <cstring>
#include <cmath>
#include <algorithm>
#include <memory>

// STB Image library implementations
#define STB_IMAGE_IMPLEMENTATION
#define STB_IMAGE_WRITE_IMPLEMENTATION

// For this example, I'll define simple image loading/saving functions
// In a real implementation, you would use STB image libraries

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
    
    // Load image from file (simplified - in reality would use STB)
    bool load(const std::string& filename) {
        // Simulated loading - creates a test pattern
        std::cout << "Loading image: " << filename << std::endl;
        
        // For demonstration, create a 256x256 RGB test image
        width = 256;
        height = 256;
        channels = 3;
        data.resize(width * height * channels);
        
        // Create a gradient test pattern
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = (y * width + x) * channels;
                data[index] = x;     // R
                data[index + 1] = y; // G
                data[index + 2] = 128; // B
            }
        }
        
        std::cout << "Image loaded: " << width << "x" << height << " with " << channels << " channels" << std::endl;
        return true;
    }
    
    // Save image to file (simplified)
    bool save(const std::string& filename) {
        std::cout << "Saving image: " << filename << std::endl;
        std::cout << "Image saved: " << width << "x" << height << " with " << channels << " channels" << std::endl;
        return true;
    }
    
    // Convert to grayscale
    void toGrayscale() {
        if (channels != 3 && channels != 4) {
            std::cerr << "Image must be RGB or RGBA" << std::endl;
            return;
        }
        
        std::vector<unsigned char> grayData(width * height);
        
        for (int i = 0; i < width * height; i++) {
            int srcIndex = i * channels;
            // Use luminance formula: 0.299*R + 0.587*G + 0.114*B
            float gray = 0.299f * data[srcIndex] + 
                        0.587f * data[srcIndex + 1] + 
                        0.114f * data[srcIndex + 2];
            grayData[i] = static_cast<unsigned char>(gray);
        }
        
        data = grayData;
        channels = 1;
        std::cout << "Converted to grayscale" << std::endl;
    }
    
    // Resize image using nearest neighbor
    void resize(int newWidth, int newHeight) {
        std::vector<unsigned char> newData(newWidth * newHeight * channels);
        
        float xRatio = static_cast<float>(width) / newWidth;
        float yRatio = static_cast<float>(height) / newHeight;
        
        for (int y = 0; y < newHeight; y++) {
            for (int x = 0; x < newWidth; x++) {
                int srcX = static_cast<int>(x * xRatio);
                int srcY = static_cast<int>(y * yRatio);
                
                for (int c = 0; c < channels; c++) {
                    int srcIndex = (srcY * width + srcX) * channels + c;
                    int dstIndex = (y * newWidth + x) * channels + c;
                    newData[dstIndex] = data[srcIndex];
                }
            }
        }
        
        data = newData;
        width = newWidth;
        height = newHeight;
        std::cout << "Resized to " << width << "x" << height << std::endl;
    }
    
    // Apply box blur
    void blur(int radius) {
        if (radius <= 0) return;
        
        std::vector<unsigned char> blurred(data.size());
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                for (int c = 0; c < channels; c++) {
                    int sum = 0;
                    int count = 0;
                    
                    // Apply box filter
                    for (int dy = -radius; dy <= radius; dy++) {
                        for (int dx = -radius; dx <= radius; dx++) {
                            int nx = x + dx;
                            int ny = y + dy;
                            
                            if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                                sum += data[(ny * width + nx) * channels + c];
                                count++;
                            }
                        }
                    }
                    
                    blurred[(y * width + x) * channels + c] = sum / count;
                }
            }
        }
        
        data = blurred;
        std::cout << "Applied blur with radius " << radius << std::endl;
    }
    
    // Adjust brightness
    void adjustBrightness(float factor) {
        for (size_t i = 0; i < data.size(); i++) {
            int newValue = static_cast<int>(data[i] * factor);
            data[i] = static_cast<unsigned char>(std::clamp(newValue, 0, 255));
        }
        std::cout << "Adjusted brightness by factor " << factor << std::endl;
    }
    
    // Flip horizontally
    void flipHorizontal() {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width / 2; x++) {
                for (int c = 0; c < channels; c++) {
                    int leftIndex = (y * width + x) * channels + c;
                    int rightIndex = (y * width + (width - 1 - x)) * channels + c;
                    std::swap(data[leftIndex], data[rightIndex]);
                }
            }
        }
        std::cout << "Flipped horizontally" << std::endl;
    }
    
    // Flip vertically
    void flipVertical() {
        for (int y = 0; y < height / 2; y++) {
            for (int x = 0; x < width; x++) {
                for (int c = 0; c < channels; c++) {
                    int topIndex = (y * width + x) * channels + c;
                    int bottomIndex = ((height - 1 - y) * width + x) * channels + c;
                    std::swap(data[topIndex], data[bottomIndex]);
                }
            }
        }
        std::cout << "Flipped vertically" << std::endl;
    }
    
    // Get image info
    void printInfo() const {
        std::cout << "Image info: " << width << "x" << height 
                  << ", " << channels << " channels, "
                  << data.size() << " bytes" << std::endl;
    }
};

// Main program
int main(int argc, char* argv[]) {
    std::cout << "=== C++ Image Processor ===" << std::endl;
    std::cout << "A simple image processing program" << std::endl << std::endl;
    
    if (argc < 2) {
        std::cout << "Usage: " << argv[0] << " <command> [options]" << std::endl;
        std::cout << "\nCommands:" << std::endl;
        std::cout << "  load <filename>           - Load an image" << std::endl;
        std::cout << "  grayscale                 - Convert to grayscale" << std::endl;
        std::cout << "  resize <width> <height>   - Resize image" << std::endl;
        std::cout << "  blur <radius>             - Apply blur filter" << std::endl;
        std::cout << "  brightness <factor>       - Adjust brightness (0.5 = darker, 2.0 = brighter)" << std::endl;
        std::cout << "  flip <h|v>                - Flip horizontal or vertical" << std::endl;
        std::cout << "  save <filename>           - Save the image" << std::endl;
        std::cout << "  demo                      - Run a demonstration" << std::endl;
        return 1;
    }
    
    std::string command = argv[1];
    
    if (command == "demo") {
        // Run a demonstration
        std::cout << "Running demonstration..." << std::endl << std::endl;
        
        Image img;
        
        // Load test image
        img.load("test.jpg");
        img.printInfo();
        std::cout << std::endl;
        
        // Create a copy for grayscale conversion
        Image grayImg = img;
        grayImg.toGrayscale();
        grayImg.save("test_gray.jpg");
        grayImg.printInfo();
        std::cout << std::endl;
        
        // Resize
        Image resizedImg = img;
        resizedImg.resize(128, 128);
        resizedImg.save("test_resized.jpg");
        resizedImg.printInfo();
        std::cout << std::endl;
        
        // Blur
        Image blurredImg = img;
        blurredImg.blur(3);
        blurredImg.save("test_blurred.jpg");
        std::cout << std::endl;
        
        // Brightness adjustment
        Image brightImg = img;
        brightImg.adjustBrightness(1.5);
        brightImg.save("test_bright.jpg");
        std::cout << std::endl;
        
        // Flip
        Image flippedImg = img;
        flippedImg.flipHorizontal();
        flippedImg.save("test_flipped.jpg");
        std::cout << std::endl;
        
        std::cout << "Demonstration complete!" << std::endl;
    }
    else {
        // Interactive mode would go here
        std::cout << "Interactive mode not implemented in this demo." << std::endl;
        std::cout << "Run with 'demo' argument to see a demonstration." << std::endl;
    }
    
    return 0;
}