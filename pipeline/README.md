# Data pipeline (Phase 3), run on the EC2 box

    sudo apt install -y python3-venv      # if missing
    python3 -m venv ~/dict-data/venv && source ~/dict-data/venv/bin/activate
    pip install -r requirements.txt
    ./01_download.sh
    python 02_build_db.py --xml ~/dict-data/raw/english-wordnet-2025-plus.xml.gz --out ~/dict-data/build/dictionary.db
    python 03_quality_gates.py ~/dict-data/build/dictionary.db

Sources: Open English WordNet 2025+ (CC BY 4.0); wordfreq for ranking (data CC BY-SA 4.0).
