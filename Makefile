CXX = g++
CXXFLAGS = -std=c++17 -Wall -Wextra -O2
LDFLAGS = -lm

# Targets
TARGETS = image_processor image_processor_advanced

all: $(TARGETS)

# Simple image processor
image_processor: image_processor.cpp
	$(CXX) $(CXXFLAGS) -o $@ $< $(LDFLAGS)

# Advanced image processor
image_processor_advanced: image_processor_advanced.cpp
	$(CXX) $(CXXFLAGS) -o $@ $< $(LDFLAGS)

# Run tests
test: $(TARGETS)
	@echo "Running simple image processor demo..."
	./image_processor demo
	@echo ""
	@echo "Running advanced image processor test..."
	./image_processor_advanced test

# Clean up
clean:
	rm -f $(TARGETS) *.bmp *.jpg *.png

# Install (optional)
install: $(TARGETS)
	cp $(TARGETS) /usr/local/bin/

.PHONY: all test clean install