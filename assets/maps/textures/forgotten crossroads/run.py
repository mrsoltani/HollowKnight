import os
import shutil
import xml.etree.ElementTree as ET
from pathlib import Path

def extract_images_from_tsx(tsx_path):
    """Parses a .tsx file and returns all image filenames referenced inside it."""
    images_in_tsx = set()
    try:
        tree = ET.parse(tsx_path)
        root = tree.getroot()
        
        # Case 1: Single image sheet tileset (<image source="..."/>)
        img_element = root.find("image")
        if img_element is not None and "source" in img_element.attrib:
            images_in_tsx.add(Path(img_element.attrib["source"]).name)

        # Case 2: Collection of individual images (<tile><image source="..."/></tile>)
        for tile in root.findall("tile"):
            tile_img = tile.find("image")
            if tile_img is not None and "source" in tile_img.attrib:
                images_in_tsx.add(Path(tile_img.attrib["source"]).name)
                    
    except Exception as e:
        print(f"  ⚠️ Error reading TSX file {tsx_path.name}: {e}")
        
    return images_in_tsx

def main():
    current_dir = Path.cwd()
    target_folder_name = "Forgotten Crossroads"
    assets_dir = current_dir / target_folder_name
    unused_dir = current_dir / "unused"
    
    # 1. Find all .tsx files
    tsx_files = [f for f in current_dir.iterdir() if f.is_file() and f.suffix.lower() == ".tsx"]
    
    if not tsx_files:
        print(f"❌ No .tsx files found in: {current_dir}\nExiting.")
        return

    if not assets_dir.exists() or not assets_dir.is_dir():
        print(f"❌ Could not find the folder '{target_folder_name}' in this directory. Exiting.")
        return

    print(f"Scanning TSX files in: {current_dir}")
    print(f"Target assets folder: {assets_dir}\n")

    # 2. Gather all expected image names from TSX files
    used_image_names = set()
    for tsx_path in tsx_files:
        used_image_names.update(extract_images_from_tsx(tsx_path))

    print(f"Total unique images needed by TSX files: {len(used_image_names)}")

    # 3. Recursively find EVERY image file inside Forgotten Crossroads and its subfolders
    valid_extensions = {".png", ".jpg", ".jpeg", ".webp"}
    all_found_images = []
    
    for root, dirs, files in os.walk(assets_dir):
        # Skip processing our own 'unused' folder if it happens to be created inside
        if "unused" in root:
            continue
        for file in files:
            file_path = Path(root) / file
            if file_path.suffix.lower() in valid_extensions:
                all_found_images.append(file_path)

    print(f"Total physical images found across all subfolders: {len(all_found_images)}")

    # 4. Separate them into used and unused lists
    images_to_keep = []
    images_to_discard = []

    for img_path in all_found_images:
        if img_path.name in used_image_names:
            images_to_keep.append(img_path)
        else:
            images_to_discard.append(img_path)

    print(f"-> Images to consolidate into '{target_folder_name}': {len(images_to_keep)}")
    print(f"-> Unused images to isolate: {len(images_to_discard)}")

    if not images_to_keep and not images_to_discard:
        print("\nNo images found to process.")
        return

    # 5. Confirmation Prompt
    print(f"\n⚠️ Action summary:")
    print(f"  - {len(images_to_keep)} used images will be flattened directly into '{target_folder_name}/'")
    print(f"  - {len(images_to_discard)} unused images will be moved into 'unused/'")
    
    confirm = input("\nProceed with organizing? (y/n): ").strip().lower()
    
    if confirm == 'y':
        # Create the unused folder
        unused_dir.mkdir(exist_ok=True)
        
        # Move used images to the root of Forgotten Crossroads
        print("\nConsolidating used images...")
        for img_path in images_to_keep:
            dest_path = assets_dir / img_path.name
            # Avoid moving a file onto itself if it's already in the root folder
            if img_path != dest_path:
                shutil.move(str(img_path), str(dest_path))

        # Move unused images to the unused folder
        print("Isolating unused images...")
        for img_path in images_to_discard:
            dest_path = unused_dir / img_path.name
            # Resolve potential duplicate filename conflicts in the unused folder
            if dest_path.exists():
                dest_path = unused_dir / f"{img_path.stem}_duplicate{img_path.suffix}"
            shutil.move(str(img_path), str(dest_path))

        print(f"\nSuccessfully finished! Check your '{target_folder_name}' and 'unused' folders.")
    else:
        print("\n❌ Cancelled. No files were moved.")

if __name__ == "__main__":
    main()