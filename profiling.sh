#!/bin/bash
adb logcat -c && gtimeout 30 adb logcat | grep "D/CameraCompat"  > test/filter_front.log
cat test/filter_front.log | grep preDraw | cut -d ":" -f 3 | cut -d "," -f 1 | grep -E "[1-9]+" | awk '{s+=$1}END{print "preDraw:",s/NR}' RS=" "
cat test/filter_front.log | grep draw | cut -d ":" -f 3 | cut -d "," -f 1 | grep -E "[1-9]+" | awk '{s+=$1}END{print "draw:",s/NR}' RS=" "
