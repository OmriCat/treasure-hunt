#!/usr/bin/env bash
inputfile="titles.txt"
while read -r line; do
  qrtool encode --type svg "$line" > "$line.svg"
done < "$inputfile"