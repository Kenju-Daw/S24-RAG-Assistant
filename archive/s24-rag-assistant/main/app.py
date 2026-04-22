import os
import sys
import streamlit as st
import json
import requests
from pathlib import Path
import subprocess
import time

# Set page configuration
st.set_page_config(
    page_title="S24-RAG-Assistant",
    page_icon="ߧ",
    layout="centered",
    initial_sidebar_state="expanded"
)

# Add title and description
st.title("ߧ S24-RAG-Assistant")
st.caption("A fully private, on-device RAG personal assistant")

# GitHub integration section
st.sidebar.title("GitHub Integration")
github_token = st.sidebar.text_input("GitHub Token", type="password", help="Enter your GitHub personal access token")
repo_owner = st.sidebar.text_input("Repository Owner", value="Kenju-Daw", help="Your GitHub username")
repo_name = st.sidebar.text_input("Repository Name", value="S24-RAG-Assistant", help="Repository name")

# Model settings
st.sidebar.subheader("Model Settings")
temperature = st.sidebar.slider("Temperature", 0.0, 1.0, 0.7, 0.1)
max_tokens = st.sidebar.slider("Max Tokens", 128, 1024, 512, 64)

# Features
st.sidebar.subheader("Features")
use_knowledge_base = st.sidebar.checkbox("Use Knowledge Base", value=True)
github_remote = st.sidebar.checkbox("GitHub Remote Control", value=False)

# Initialize chat history
if "messages" not in st.session_state:
    st.session_state.messages = []

# Display chat messages
for message in st.session_state.messages:
    with st.chat_message(message["role"]):
        st.markdown(message["content"])

# Function to handle GitHub operations
def github_api_call(endpoint, method="GET", data=None):
    if not github_token:
        return None
    
    headers = {
        "Authorization": f"token {github_token}",
        "Accept": "application/vnd.github.v3+json"
    }
    
    url = f"https://api.github.com/{endpoint}"
    
    if method == "GET":
        response = requests.get(url, headers=headers)
    elif method == "POST":
        response = requests.post(url, headers=headers, json=data)
    elif method == "PATCH":
        response = requests.patch(url, headers=headers, json=data)
    
    if response.status_code == 200:
        return response.json()
    else:
        return None

# Function to push changes to GitHub
def push_to_github(message="Update from S24-RAG-Assistant"):
    try:
        # Check if we're in a git repository
        result = subprocess.run(["git", "status"], capture_output=True, text=True)
        if "fatal" in result.stderr:
            return "Not a git repository"
        
        # Add all files
        subprocess.run(["git", "add", "."], check=True)
        
        # Commit changes
        subprocess.run(["git", "commit", "-m", message], check=True)
        
        # Push to GitHub
        result = subprocess.run(["git", "push"], capture_output=True, text=True)
        
        if result.returncode == 0:
            return "Changes pushed to GitHub successfully!"
        else:
            return f"Error pushing to GitHub: {result.stderr}"
    except Exception as e:
        return f"Error: {str(e)}"

# Function to pull changes from GitHub
def pull_from_github():
    try:
        # Check if we're in a git repository
        result = subprocess.run(["git", "status"], capture_output=True, text=True)
        if "fatal" in result.stderr:
            return "Not a git repository"
        
        # Pull changes
        result = subprocess.run(["git", "pull"], capture_output=True, text=True)
        
        if result.returncode == 0:
            return "Changes pulled from GitHub successfully!"
        else:
            return f"Error pulling from GitHub: {result.stderr}"
    except Exception as e:
        return f"Error: {str(e)}"

# GitHub remote control section
if github_remote:
    st.subheader("GitHub Remote Control")
    
    col1, col2 = st.columns(2)
    
    with col1:
        if st.button("Pull Changes from GitHub"):
            result = pull_from_github()
            st.info(result)
    
    with col2:
        if st.button("Push Changes to GitHub"):
            result = push_to_github()
            st.info(result)
    
    st.markdown("""
    ### How to use GitHub integration:
    
    1. **Pull Changes**: Download the latest version from GitHub
    2. **Make Changes**: Edit files on your device
    3. **Push Changes**: Upload your changes back to GitHub
    
    Make sure you've set up Git properly on your device first.
    """)

# Accept user input
if prompt := st.chat_input("Ask a question..."):
    # Add user message to chat history
    st.session_state.messages.append({"role": "user", "content": prompt})
    with st.chat_message("user"):
        st.markdown(prompt)
    
    # Generate response
    with st.chat_message("assistant"):
        message_placeholder = st.empty()
        with st.spinner("Thinking..."):
            # Simple response for now
            response = f"I understand you asked: '{prompt}'. This is a placeholder response. The full RAG functionality will be implemented soon."
            message_placeholder.markdown(response)
    
    # Add assistant response to chat history
    st.session_state.messages.append({"role": "assistant", "content": response})

# Knowledge base management
st.subheader("Knowledge Base Management")
uploaded_files = st.file_uploader(
    "Upload documents to add to the knowledge base",
    type=["txt", "pdf"],
    accept_multiple_files=True
)

if uploaded_files and st.button("Add to Knowledge Base"):
    st.success(f"Added {len(uploaded_files)} documents to the knowledge base (placeholder)")

# Git setup instructions
with st.expander("Git Setup Instructions"):
    st.markdown("""
    ### First-time Git setup on Termux:
    
    1. **Install Git**:
       ```
       pkg install git
       ```
    
    2. **Configure Git**:
       ```
       git config --global user.name "Your Name"
       git config --global user.email "your.email@example.com"
       ```
    
    3. **Clone your repository**:
       ```
       git clone https://github.com/Kenju-Daw/S24-RAG-Assistant.git
       cd S24-RAG-Assistant
       ```
    
    4. **Run the application**:
       ```
       streamlit run app.py
       ```
    """)
