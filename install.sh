#!/bin/bash
# S24-RAG-Assistant Installation Script

set -e

echo "Installing S24-RAG-Assistant..."

# Update Termux
pkg update -y
pkg upgrade -y

# Install required packages
pkg install -y proot-distro python wget curl

# Create project directory
mkdir -p ~/s24-rag-assistant
cd ~/s24-rag-assistant

# Download the project
wget -q --show-progress -O app.py https://raw.githubusercontent.com/Kenju-Daw/S24-RAG-Assistant/main/app.py
wget -q --show-progress -O requirements.txt https://raw.githubusercontent.com/Kenju-Daw/S24-RAG-Assistant/main/requirements.txt

# Install Python requirements
pip install -r requirements.txt

# Create start script
cat > start.sh << 'EOF'
#!/bin/bash
cd ~/s24-rag-assistant
streamlit run app.py --server.port 8501 --server.address 0.0.0.0
EOF
chmod +x start.sh

echo "Installation complete!"
echo "Run './start.sh' to start the assistant"
