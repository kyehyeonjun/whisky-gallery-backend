#!/bin/sh
# ----------------------------------------------------------------------------
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
# ----------------------------------------------------------------------------

# ----------------------------------------------------------------------------
# Apache Maven Wrapper startup script, version 3.3.4
#
# Optional ENV vars
#   MVNW_REPOURL - repo url base for downloading maven distribution
#   MVNW_USERNAME/MVNW_PASSWORD - user and password for downloading maven
#   MVNW_VERBOSE - true: enable verbose log; others: silence the output
# ----------------------------------------------------------------------------

set -e

MAVEN_WRAPPER_JAR_DIR="${MAVEN_USER_HOME:-${HOME}/.m2}/wrapper/dists"
MAVEN_WRAPPER_PROPERTIES_FILE="${PWD}/.mvn/wrapper/maven-wrapper.properties"

# Calculate distributionUrl, requires .mvn/wrapper/maven-wrapper.properties
if [ -f "$MAVEN_WRAPPER_PROPERTIES_FILE" ]; then
  while IFS='=' read -r key value; do
    case "$key" in
      'distributionUrl')
        distributionUrl="$value"
        break
      ;;
    esac
  done <"$MAVEN_WRAPPER_PROPERTIES_FILE"
fi

if [ -z "$distributionUrl" ]; then
  echo "distributionUrl property not found in $MAVEN_WRAPPER_PROPERTIES_FILE" >&2
  exit 1
fi

# Apply MVNW_REPOURL and calculate MAVEN_HOME
# maven home pattern: ~/.m2/wrapper/dists/{apache-maven-<version>,maven-mvnd-<version>-<platform>}/<hash>
if [ -n "$MVNW_REPOURL" ]; then
  if echo "$distributionUrl" | grep -q 'apache-maven'; then
    MVNW_REPO_PATTERN="/org/apache/maven/"
  elif echo "$distributionUrl" | grep -q 'maven-mvnd'; then
    MVNW_REPO_PATTERN="/maven/mvnd/"
  else
    echo "distributionUrl is not valid, must contain 'apache-maven' or 'maven-mvnd'" >&2
    exit 1
  fi
  distributionUrl="$MVNW_REPOURL$MVNW_REPO_PATTERN$(echo "$distributionUrl" | sed "s|^.*$MVNW_REPO_PATTERN||")"
fi

distributionUrlName=$(basename "$distributionUrl")
distributionUrlNameMain=$(echo "$distributionUrlName" | sed 's/\.[^.]*$//' | sed 's/-bin$//')

MAVEN_HOME_PARENT="$MAVEN_WRAPPER_JAR_DIR/$distributionUrlNameMain"
MAVEN_HOME_NAME=$(echo "$distributionUrl" | sha256sum | cut -d' ' -f1)
MAVEN_HOME="$MAVEN_HOME_PARENT/$MAVEN_HOME_NAME"

if [ -d "$MAVEN_HOME" ]; then
  if [ -n "$MVNW_VERBOSE" ]; then
    echo "found existing MAVEN_HOME at $MAVEN_HOME"
  fi
else
  if [ -z "$distributionUrlNameMain" ] || [ "$distributionUrlName" = "$distributionUrlNameMain" ]; then
    echo "distributionUrl is not valid, must end with *-bin.zip, but found $distributionUrl" >&2
    exit 1
  fi

  # prepare tmp dir
  TMP_DOWNLOAD_DIR=$(mktemp -d)
  trap 'rm -rf "$TMP_DOWNLOAD_DIR"' EXIT

  mkdir -p "$MAVEN_HOME_PARENT"

  # Download and Install Apache Maven
  if [ -n "$MVNW_VERBOSE" ]; then
    echo "Couldn't find MAVEN_HOME, downloading and installing it ..."
    echo "Downloading from: $distributionUrl"
    echo "Downloading to: $TMP_DOWNLOAD_DIR/$distributionUrlName"
  fi

  if command -v curl >/dev/null 2>&1; then
    if [ -n "$MVNW_USERNAME" ] && [ -n "$MVNW_PASSWORD" ]; then
      curl --fail --location --user "$MVNW_USERNAME:$MVNW_PASSWORD" --output "$TMP_DOWNLOAD_DIR/$distributionUrlName" "$distributionUrl"
    else
      curl --fail --location --output "$TMP_DOWNLOAD_DIR/$distributionUrlName" "$distributionUrl"
    fi
  elif command -v wget >/dev/null 2>&1; then
    if [ -n "$MVNW_USERNAME" ] && [ -n "$MVNW_PASSWORD" ]; then
      wget --auth-no-challenge --user "$MVNW_USERNAME" --password "$MVNW_PASSWORD" -O "$TMP_DOWNLOAD_DIR/$distributionUrlName" "$distributionUrl"
    else
      wget -O "$TMP_DOWNLOAD_DIR/$distributionUrlName" "$distributionUrl"
    fi
  else
    echo "Neither curl nor wget are available." >&2
    exit 1
  fi

  # If specified, validate the SHA-256 sum of the Maven distribution zip file
  if [ -f "$MAVEN_WRAPPER_PROPERTIES_FILE" ]; then
    while IFS='=' read -r key value; do
      case "$key" in
        'distributionSha256Sum')
          distributionSha256Sum="$value"
          break
        ;;
      esac
    done <"$MAVEN_WRAPPER_PROPERTIES_FILE"
  fi

  if [ -n "$distributionSha256Sum" ]; then
    if echo "$distributionUrl" | grep -q 'maven-mvnd'; then
      echo "Checksum validation is not supported for maven-mvnd. Please disable validation by removing 'distributionSha256Sum' from your maven-wrapper.properties." >&2
      exit 1
    fi
    downloadSha256Sum=$(sha256sum "$TMP_DOWNLOAD_DIR/$distributionUrlName" | cut -d' ' -f1)
    if [ "$downloadSha256Sum" != "$distributionSha256Sum" ]; then
      echo "Error: Failed to validate Maven distribution SHA-256, your Maven distribution might be compromised. If you updated your Maven version, you need to update the specified distributionSha256Sum property." >&2
      exit 1
    fi
  fi

  # unzip and move
  unzip -q "$TMP_DOWNLOAD_DIR/$distributionUrlName" -d "$TMP_DOWNLOAD_DIR"

  # Find the actual extracted directory name (handles snapshots where filename != directory name)
  actualDistributionDir=""

  # First try the expected directory name (for regular distributions)
  expectedPath="$TMP_DOWNLOAD_DIR/$distributionUrlNameMain"
  if [ -d "$expectedPath" ] && [ -x "$expectedPath/bin/mvn" ]; then
    actualDistributionDir="$distributionUrlNameMain"
  fi

  # If not found, search for any directory with the Maven executable (for snapshots)
  if [ -z "$actualDistributionDir" ]; then
    for dir in "$TMP_DOWNLOAD_DIR"/*; do
      if [ -d "$dir" ] && [ -x "$dir/bin/mvn" ]; then
        actualDistributionDir=$(basename "$dir")
        break
      fi
    done
  fi

  if [ -z "$actualDistributionDir" ]; then
    echo "Could not find Maven distribution directory in extracted archive" >&2
    exit 1
  fi

  if [ -n "$MVNW_VERBOSE" ]; then
    echo "Found extracted Maven distribution directory: $actualDistributionDir"
  fi

  mv "$TMP_DOWNLOAD_DIR/$actualDistributionDir" "$TMP_DOWNLOAD_DIR/$MAVEN_HOME_NAME"
  mv "$TMP_DOWNLOAD_DIR/$MAVEN_HOME_NAME" "$MAVEN_HOME_PARENT"
fi

# Define the MVN_CMD variable
if echo "$distributionUrl" | grep -q 'maven-mvnd'; then
  MVN_CMD="$MAVEN_HOME/bin/mvnd"
else
  MVN_CMD="$MAVEN_HOME/bin/mvn"
fi

# Execute Maven
exec "$MVN_CMD" "$@"
