#!/bin/bash
ant -silent -propertyfile taloswebservice.properties -buildfile taloswebservice.xml || { echo "Ant build failed"; exit 1; }
rm -rf out || { echo "Remove of Build failed"; exit 1; }