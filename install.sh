#!/bin/bash
# S24-RAG-Assistant Installation Script

set -e

echo "Installing S24-RAG-Assistant..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Update Termux
print_status "Updating Termux packages..."
pkg update -y
pkg upgrade -y

# Install required packages
print_status "Installing required packages..."
pkg install -y python git wget curl

# Create project directory
print_status "Creating project directory..."
mkdir -p ~/s24-rag-assistant
cd ~/s24-rag-assistant

# Function to download file with fallback
download_file() {
    local file_name=$1
    local file_url=$2
    local fallback_content=$3
    
    print_status "Downloading $file_name..."
    
    if wget -q --show-progress -O "$file_name" "$file_url"; then
        print_success "$file_name downloaded successfully"
    else
        print_warning "Failed to download $file_name, creating locally..."
        echo "$fallback_content" > "$file_name"
        print_success "$file_name created locally"
    fi
}

# Download app.py
download_file "app.py" \
    "https://raw.githubusercontent.com/Kenju-Daw/S24-RAG-Assistant/main/app.py" \
'import os
import sys
import streamlit as st

st.set_page_config(
    page_title="S24-RAG-Assistant",
    page_icon="ߧ",
    layout="centered"
)

st.title("ߧ S24-RAG-Assistant")
st.caption("A fully private, on-device RAG personal assistant")

st.write("Welcome to your personal AI assistant!")
st.write("This is a basic version to get you started.")

if "messages" not in st.session_state:
    st.session_state.messages = []

for message in st.session_state.messages:
    with st.chat_message(message["role"]):
        st.markdown(message["content"])

if prompt := st.chat_input("Ask a question..."):
    st.session_state.messages.append({"role": "user", "content": prompt})
    with st.chat_message("user"):
        st.markdown(prompt)
    
    with st.chat_message("assistant"):
        st.markdown(f"I understand you asked: {prompt}")
    
    st.session_state.messages.append({"role": "assistant", "content": f"I understand you asked: {prompt}"})
'

# Download requirements.txt
download_file "requirements.txt" \
    "https://raw.githubusercontent.com/Kenju-Daw/S24-RAG-Assistant/main/requirements.txt" \
'streamlit==1.28.0
requests==2.31.0'

# Check if requirements.txt exists and has content
if [ ! -s requirements.txt ]; then
    print_error "requirements.txt is empty or doesn't exist"
    print_status "Creating minimal requirements.txt..."
    echo 'streamlit==1.28.0' > requirements.txt
fi

# Install Python packages
print_status "Installing Python packages..."
if pip install -r requirements.txt; then
    print_success "Python packages installed successfully"
else
    print_error "Failed to install Python packages from requirements.txt"
    print_status "Trying to install minimal packages..."
    pip install streamlit requests
    print_success "Minimal packages installed"
fi

# Create start script
print_status "Creating start script..."
cat > start.sh << 'END'
#!/bin/bash
cd ~/s24-rag-assistant
streamlit run app.py --server.port 8501 --server.address 0.0.0.0
END
chmod +x start.sh

# Create update script
print_status "Creating update script..."
cat > update.sh << 'END'
#!/bin/bash
cd ~/s24-rag-assistant
print_status "Updating S24-RAG-Assistant..."

# Backup current version
print_status "Backing up current version..."
cp app.py app.py.backup
cp requirements.txt requirements.txt.backup

# Download updates
print_status "Downloading updates..."
wget -q --show-progress -O app.py.new https://raw.githubusercontent.com/Kenju-Daw/S24-RAG-Assistant/main/app.py
wget -q --show-progress -O requirements.txt.new https://raw.githubusercontent.com/Kenju-Daw/S24-RAG-Assistant/main/requirements.txt

# Check if downloads were successful
if [ -s app.py.new ] && [ -s requirements.txt.new ]; then
    # Replace old files
    mv app.py.new app.py
    mv requirements.txt.new requirements.txt
    
    # Update Python packages
    print_status "Updating Python packages..."
    pip install -r requirements.txt
    
    print_success "Update completed successfully!"
else
    print_error "Update failed, restoring backup..."
    mv app.py.backup app.py
    mv requirements.txt.backup requirements.txt
    print_success "Backup restored"
fi

# Clean up
rm -f app.py.new requirements.txt.new app.py.backup requirements.txt.backup
END
chmod +x update.sh

# Installation complete
echo ""
echo -e "${GREEN}===========================================${NC}"
echo -e "${GREEN}  Installation Complete!                    ${NC}"
echo -e "${GREEN}===========================================${NC}"
echo ""
echo -e "${BLUE}To start the assistant:${NC}"
echo -e "${YELLOW}  cd ~/s24-rag-assistant${NC}"
echo -e "${YELLOW}  ./start.sh${NC}"
echo ""
echo -e "${BLUE}To update the assistant:${NC}"
echo -e "${YELLOW}  cd ~/s24-rag-assistant${NC}"
echo -e "${YELLOW}  ./update.sh${NC}"
echo ""
echo -e "${BLUE}Then open your browser and go to:${NC}"
echo -e "${YELLOW}  http://localhost:8501${NC}"
echo ""
echo -e "${GREEN}===========================================${NC}"
