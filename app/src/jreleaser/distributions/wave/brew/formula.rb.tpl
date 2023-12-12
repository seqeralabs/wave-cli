# {{jreleaserCreationStamp}}
class {{brewFormulaName}} < Formula
    desc "{{projectDescription}}"
    homepage "{{projectLinkHomepage}}"
    version "{{projectVersion}}"
    license "{{projectLicense}}"

    if OS.mac? && Hardware::CPU.arm?
        url "https://github.com/seqeralabs/wave-cli/releases/download/v{{projectEffectiveVersion}}/wave-{{projectEffectiveVersion}}-macos-arm64.zip"
        sha256 "{{distributionChecksumSha256}}"

        def install
            bin.install "{{distributionExecutableName}}" => "{{distributionExecutableName}}"
        end

        test do
            output = shell_output("#{bin}/{{distributionExecutableName}} --version")
            assert_match "{{projectVersion}}", output
        end
    end

    if OS.mac? && Hardware::CPU.intel?
        url "https://github.com/seqeralabs/wave-cli/releases/download/v{{projectEffectiveVersion}}/wave-{{projectEffectiveVersion}}-macos-x86_64.zip"
        sha256 "{{distributionChecksumSha256}}"

        def install
            bin.install "{{distributionExecutableName}}" => "{{distributionExecutableName}}"
        end

        test do
            output = shell_output("#{bin}/{{distributionExecutableName}} --version")
            assert_match "{{projectVersion}}", output
        end
    end
end
