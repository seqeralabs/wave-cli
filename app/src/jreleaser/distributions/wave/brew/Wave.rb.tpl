# Homebrew Formula for Wave-cli

class Wave < Formula
  desc "cli for wave"
  homepage "https://seqera.io/wave"
  url "https://github.com/seqeralabs/wave-cli/releases/download/v{{projectEffectiveVersion}}/wave-{{projectEffectiveVersion}}-macos-arm64.zip"
  license "Apache-2.0"

  def install
    unzip wave-{{projectEffectiveVersion}}-macos-x86_64.zip
    mv "wave-{{projectEffectiveVersion}}-macos-x86_64", "wave"
    bin.install "wave"
  end

  test do
    system "#{bin}/wave", "--version"
  end

end