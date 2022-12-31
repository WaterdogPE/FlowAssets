/*
 * Copyright 2022 WaterdogTEAM
 * Licensed under the GNU General Public License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.waterdog.flowassets.client;

import dev.waterdog.flowassets.client.structure.AssetDownloadData;
import dev.waterdog.flowassets.client.structure.GroupDownload;
import dev.waterdog.flowassets.client.structure.UpdateResponseData;
import dev.waterdog.flowassets.client.structure.UploadResponseData;
import joptsimple.BuiltinHelpFormatter;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class Bootstrap {

    private static final OptionParser PARSER = new OptionParser();
    static {
        PARSER.allowsUnrecognizedOptions();
        PARSER.formatHelpWith(new BuiltinHelpFormatter(80, 2){
            @Override
            protected String formattedHelpOutput() {
                String heading = "FlowAssets bash tool by WaterdogPE\n" +
                        "View below for details of the available settings.\n\n";
                return heading + super.formattedHelpOutput();
            }
        });
    }

    private static final OptionSpec<Void> HELP_OPTION = PARSER.acceptsAll(Arrays.asList("?", "h", "help"), "Shows help page")
            .forHelp();

    private static final OptionSpec<String> SERVER_OPTION = PARSER.accepts("server", "FlowAssets server address")
            .requiredUnless("help")
            .withRequiredArg()
            .ofType(String.class);

    private static final OptionSpec<String> TOKEN_OPTION = PARSER.accepts("token", "Access token used for requests")
            .requiredUnless("help")
            .withRequiredArg()
            .ofType(String.class);

    private static final OptionSpec<String> ASSET_OPTION = PARSER.accepts("asset", "Name of the asset")
            // TODO: .requiredUnless("help", "group")
            .withRequiredArg()
            .ofType(String.class);

    private static final OptionSpec<String> GROUP_OPTION = PARSER.accepts("group", "Name of the asset group")
            .requiredUnless("help", "asset")
            .withRequiredArg()
            .ofType(String.class);

    private static final OptionSpec<Void> DOWNLOAD_OPTION = PARSER.accepts("download", "Flag to download asset or group of assets");
    // TODO:  .requiredUnless("help", "upload", "update");

    private static final OptionSpec<Void> UPLOAD_OPTION = PARSER.accepts("upload", "Flag to upload asset");
    // TODO:  .requiredUnless("help", "download", "update");

    private static final OptionSpec<Void> UPDATE_OPTION = PARSER.accepts("update", "Flag to update asset file")
            .requiredUnless("help", "download", "upload");

    private static final OptionSpec<String> FILE_OPTION = PARSER.accepts("file", "Path of the file to download to or upload from")
            .requiredIf("upload", "update")
            .withRequiredArg()
            .ofType(String.class);

    private static final OptionSpec<String> REPOSITORY_OPTION = PARSER.accepts("repository", "Asset repository name for uploading")
            .requiredIf("upload")
            .withRequiredArg()
            .ofType(String.class);

    public static void main(String[] args) throws Exception {
        OptionSet options;
        if (args.length == 0 || (options = PARSER.parse(args)).has(HELP_OPTION)) {
            PARSER.printHelpOn(System.out);
            return;
        }

        FlowAssetResolver resolver = new FlowAssetResolver(options.valueOf(SERVER_OPTION), options.valueOf(TOKEN_OPTION));
        if (options.has(DOWNLOAD_OPTION)) {
            download(resolver, options);
        } else if (options.has(UPLOAD_OPTION)) {
            upload(resolver, options);
        } else if (options.has(UPDATE_OPTION)) {
            update(resolver, options);
        }
        resolver.close();
    }

    private static void download(FlowAssetResolver resolver, OptionSet options) throws Exception {
        if (options.has(ASSET_OPTION)) {
            String assetName = options.valueOf(ASSET_OPTION);
            Path downloadPath = options.has(FILE_OPTION) ? Paths.get(options.valueOf(FILE_OPTION)) : null;
            resolver.downloadAsset(assetName, downloadPath).join();
            log.info("Downloaded {} successfully!", assetName);
            return;
        }

        String groupName = options.valueOf(GROUP_OPTION);
        GroupDownload download = resolver.downloadGroup(groupName).join();

        AtomicInteger counter = new AtomicInteger();
        for (GroupDownload.Download asset : download.getDownloads()) {
            asset.getFuture().whenComplete((i, error) -> {
                AssetDownloadData data = asset.getAssetData();
                if (error != null) {
                    log.error("Failed to download asset {}", data.getAssetName(), error);
                } else {
                    int count = counter.incrementAndGet();
                    double time = Math.round((double) (System.currentTimeMillis() - asset.getStartTime()) / 10) / 100D;
                    log.info("[{}/{}] Downloaded asset {} in {} seconds!", count, download.getDownloads().size(),
                            data.getAssetName(), time);
                }
            });
        }

        download.getFuture().join();
    }

    private static void upload(FlowAssetResolver resolver, OptionSet options) throws Exception {
        if (!options.has(ASSET_OPTION) || !options.has(REPOSITORY_OPTION) || !options.has(FILE_OPTION)) {
            PARSER.printHelpOn(System.out);
            return;
        }

        String assetName = options.valueOf(ASSET_OPTION);
        String repositoryName = options.valueOf(REPOSITORY_OPTION);
        Path path = Paths.get(options.valueOf(FILE_OPTION));
        UploadResponseData responseData = resolver.uploadAsset(assetName, repositoryName, path).join();
        if (responseData.isSuccess()) {
            log.info("Uploaded asset {} with uuid {} successfully!", assetName, responseData.getAssetUuid());
        } else {
            log.warn("Asset {} upload failed with message {}", assetName, responseData.getMessage());
        }
    }

    private static void update(FlowAssetResolver resolver, OptionSet options) throws Exception {
        if (!options.has(ASSET_OPTION) || !options.has(FILE_OPTION)) {
            PARSER.printHelpOn(System.out);
            return;
        }

        String assetName = options.valueOf(ASSET_OPTION);
        Path path = Paths.get(options.valueOf(FILE_OPTION));
        UpdateResponseData responseData = resolver.updateAsset(assetName, path).join();
        if ("ok".equals(responseData.getStatus())) {
            log.info("Updated asset {} successfully!", assetName);
        } else {
            log.warn("Asset {} update failed with message {}", assetName, responseData.getMessage());
        }
    }
}
