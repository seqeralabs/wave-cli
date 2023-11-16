# Homebrew Formula for Wave-cli

class wave < Formula
  desc "cli for wave"
  homepage "https://github.com/seqeralabs/wave-cli"
  url "https://github.com/seqeralabs/wave-cli/releases/download/v{{projectEffectiveVersion}}/wave-{{projectEffectiveVersion}}-macos-x86_64"
  license "Apache-2.0"

  depends_on macos: ">= :catalina"

    def install
      bin.install "wave"
    end

    test do
      system "#{bin}/wave", "--version"
    end
  end

end