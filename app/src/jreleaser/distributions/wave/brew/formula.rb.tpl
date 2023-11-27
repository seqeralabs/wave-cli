# Homebrew Formula for Wave-cli

class Wave < Formula
  desc "cli for wave"
  homepage "https://seqera.io/wave"
  url "https://github.com/seqeralabs/wave-cli/releases/download/v{{projectEffectiveVersion}}/wave-{{projectEffectiveVersion}}-macos-arm64"
  license "Apache-2.0"

  def install
    mv "wave-{{projectEffectiveVersion}}-macos-arm64", "wave"
    bin.install "wave"
  end

  test do
    system "#{bin}/wave", "--version"
  end

end