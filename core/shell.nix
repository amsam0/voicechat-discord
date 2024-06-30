with import <nixpkgs> { };
mkShell {
  buildInputs = [
    libopus
    pkg-config
  ] ++ lib.optional stdenv.hostPlatform.isDarwin darwin.libiconv;
}
