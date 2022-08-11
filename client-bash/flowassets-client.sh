#!/usr/bin/env bash
#
# Copyright 2022 WaterdogTEAM
# Licensed under the GNU General Public License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# https://www.gnu.org/licenses/old-licenses/gpl-2.0.html
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# Exit on error
set -e

function show_help() {
        echo "FlowAssets bash tool by WaterdogPE"
        echo "View below for details of the available settings."
        echo  ""
        echo "Settings:"
        echo "  * --server | FlowAssets server address"
        echo "  * --token | Access token used for requests"
        echo "  * --asset | Name of the asset"
        echo "  * --download | Flag to download asset"
        echo "  * --upload | Flag to upload asset"
        echo "  * --update | Flag to update asset file"
        echo "  * --file | Path of the file to download to or upload from"
        echo "  * --repository | Asset repository name for uploading"
}

# Downloads the asset using FlowAssets API
function download_asset() {
  echo "Downloading asset $ASSET_IDENTIFIER ..."
  asset_info=$(curl -s --connect-timeout 15 --header "flow-auth-token: $ACCESS_TOKEN" $SERVER_ADDRESS/api/asset/name/"$ASSET_IDENTIFIER")
  success=$(jq -r '.valid' <<< "$asset_info")
  download_url=$(jq -r '.downloadLink' <<< "$asset_info")

  if [[ -z "$success" || "true" != "$success" || -z "$download_url" ]]; then
    echo "Failed to download asset $ASSET_IDENTIFIER"
    exit 1
  fi

  deploy_path=$(jq -r '.deployPath' <<< "$asset_info")
  if [[ -z "$deploy_path" || "null" == "$deploy_path" ]]
  then
    deploy_path="./"
  else
    deploy_path="${deploy_path}"
    mkdir -p "$deploy_path"
  fi

  if [[ -z "$FILE_PATH" ]]
  then
    file_extension=$(jq -r '.fileName' <<< "$asset_info")
    download_path="${deploy_path}${file_extension}"
  else
    download_path="${deploy_path}${FILE_PATH}"
  fi

  echo "Found an asset with UUID '$(jq -r '.uuid' <<< "$asset_info")' downloading to $download_path ..."

  if [[ "$download_url" == /api/file/* ]]
  then
    curl -o "$download_path" --header "flow-auth-token: $ACCESS_TOKEN" "${SERVER_ADDRESS}${download_url}"
  else
    curl -o "$download_path" "$download_url"
  fi

  echo "Downloaded asset '$ASSET_IDENTIFIER' successfully!"
}

# Upload the asset to FlowAsset service
function upload_asset() {
  upload_response=$(curl -s --connect-timeout 15 -F asset_name="$ASSET_IDENTIFIER" -F repository="$REPOSITORY_NAME" -F attachment="@$FILE_PATH" --header "flow-auth-token: $ACCESS_TOKEN" "$SERVER_ADDRESS/api/asset/upload")
  success=$(jq -r '.success' <<< "$upload_response")
  asset_uuid=$(jq -r '.assetUuid' <<< "$upload_response")
  if [[ -z "$success" || "true" != "$success" || -z "$asset_uuid" ]]; then
      echo "Failed to upload asset $ASSET_IDENTIFIER"
      exit 1
  fi

  echo "Uploaded asset $ASSET_IDENTIFIER successfully! Asset UUID is '$asset_uuid'"
}

function update_asset() {
  update_response=$(curl --connect-timeout 15 -F asset_name="$ASSET_IDENTIFIER" -F attachment="@$FILE_PATH" --header "flow-auth-token: $ACCESS_TOKEN" "$SERVER_ADDRESS/api/asset/update")
  success=$(jq -r '.status' <<< "$update_response")
  if [[ -z "$success" || "ok" != "$success" ]]; then
        echo "Failed to update asset $ASSET_IDENTIFIER"
        exit 1
  fi
  echo "Updated asset $ASSET_IDENTIFIER successfully!"
}

function check_flag() {
  if [[ ! -z "$HAS_FLAG" ]]; then
    echo "Can not combine download/upload/update flags!"
    exit 1
  fi
  HAS_FLAG=true
}

# Parse arguments
for i in "$@"; do
  case $i in
    --server=*)
      SERVER_ADDRESS="${i#*=}"
      shift
      ;;
    --token=*)
      ACCESS_TOKEN="${i#*=}"
      shift
      ;;
    --asset*)
      ASSET_IDENTIFIER="${i#*=}"
      shift
      ;;
    --file*)
      FILE_PATH="${i#*=}"
      shift
      ;;
    --repository*)
      REPOSITORY_NAME="${i#*=}"
      shift
      ;;
    --download)
      check_flag
      DOWNLOAD_FLAG=true
      shift
      ;;
    --upload)
      check_flag
      UPLOAD_FLAG=true
      shift
      ;;
    --update)
      check_flag
      UPDATE_FLAG=true
      shift
      ;;
    -*|--*)
      echo "Unknown option $i"
      show_help
      exit 1
      ;;
    *)
      ;;
  esac
done

if [[ -z "$SERVER_ADDRESS" || -z "$ACCESS_TOKEN" || -z "$ASSET_IDENTIFIER" ]]; then
  show_help
  exit 1
fi

if [[ ! -z "$DOWNLOAD_FLAG" ]]; then
  download_asset
fi

if [[ ! -z "$UPLOAD_FLAG" ]]; then
  if [[ -z "$REPOSITORY_NAME" ]]; then
    echo "No upload repository specified!"
    show_help
    exit 1
  fi
  if [[ -z "$FILE_PATH" ]]; then
    echo "No upload file path specified!"
    show_help
    exit 1
  fi
  upload_asset
fi

if [[ ! -z "$UPDATE_FLAG" ]]; then
  if [[ -z "$FILE_PATH" ]]; then
    echo "No upload file path specified!"
    show_help
    exit 1
  fi
  update_asset
fi
