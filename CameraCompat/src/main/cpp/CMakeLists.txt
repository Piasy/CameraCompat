cmake_minimum_required(VERSION 3.4.1)

add_library(rgb-yuv-converter-library SHARED
            RgbYuvEncoder.c)

# Include libraries needed for rgb-yuv-converter-library
target_link_libraries(rgb-yuv-converter-library
                      android
                      log)
