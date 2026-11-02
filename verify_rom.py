import hashlib
import os

def get_sha256(file_path):
    sha256_hash = hashlib.sha256()
    with open(file_path, "rb") as f:
        for byte_block in iter(lambda: f.read(4096), b""):
            sha256_hash.update(byte_block)
    return sha256_hash.hexdigest()

original_rom = "rom_extracted/Super Mario 64 (USA).z64"
asset_rom = "app/src/main/assets/rom/Super Mario 64 (USA).z64"

print(f"Original ROM: {original_rom}")
print(f"Asset ROM: {asset_rom}")

if os.path.exists(asset_rom):
    original_hash = get_sha256(original_rom)
    asset_hash = get_sha256(asset_rom)
    
    print(f"Original Hash: {original_hash}")
    print(f"Asset Hash:    {asset_hash}")
    
    if original_hash == asset_hash:
        print("VERIFICATION SUCCESS: ROM integrity preserved!")
    else:
        print("VERIFICATION FAILED: ROM integrity corrupted!")
else:
    print("VERIFICATION FAILED: ROM not found in assets!")
