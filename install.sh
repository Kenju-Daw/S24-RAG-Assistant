#!/bin/bash
# S24-RAG-Assistant Installation Script

set -e

echo "Installing S24-RAG-Assistant..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Update Termux
echo -e "${YELLOW}Updating Termux packages...${NC}"
pkg update -y
pkg upgrade -y

# Install required packages
echo -e "${YELLOW}Installing required packages...${NC}"
pkg install -y python git wget curl

# Install Python packages
echo -e "${YELLOW}Installing Python packages...${NC}"
pip install -r requirements.txt

# Create start script
echo -e "${YELLOW}Creating start script...${NC}"
cat > start.sh << 'END'
#!/bin/bash
cd ~/s24-rag-assistant
streamlit run app.py --server.port 8501 --server.address 0.0.0.0
END
chmod +x start.sh

# Installation complete
echo -e "${GREEN}Installation complete!${NC}"
echo -e "${GREEN}Run './start.sh' to start the assistant${NC}"
