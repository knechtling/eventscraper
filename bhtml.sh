#/bin/bash
set -x
url=$1
html=$(curl "$url" | jq -r '.content.rendered' | tidy -indent --drop-empty-elements no)
echo "$html" > unfucked.html
