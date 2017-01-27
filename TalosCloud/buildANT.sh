#!/bin/bash
ant -silent -buildfile taloscloud.xml || { echo "Ant build failed"; exit 1; }