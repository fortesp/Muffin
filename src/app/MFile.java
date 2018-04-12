package app;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import java.util.Set;
import java.util.HashSet;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/*
    Muffin Project v0.1
    Author: Pedro Fortes (c) 2017
    https://github.com/fortesp
 */
public final class MFile extends File {

    private String name;
    private String extension;

    private MFile parentMFile;
    private Set<MFile> linkedfiles = new HashSet<MFile>();

    private long length = 0;

    public MFile(File file) {

        this(file.getPath());
    }

    public MFile(String path) {

        super(path);

        this.name = super.getName();
        this.length = super.length();
        this.extension = FilenameUtils.getExtension(this.getName()).toLowerCase();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public long length() {
        return length;
    }

    public MFile getParentMFile() {
        return parentMFile;
    }

    public void setParentMFile(MFile parentMFile) {
        this.parentMFile = parentMFile;
    }

    public Set<MFile> getLinkedFiles() {
        return linkedfiles;
    }

    protected void addLinkedFile(MFile file) {
        this.linkedfiles.add(file);
    }

    public String getExtension() {
        return extension;
    }

    public boolean contentEquals(MFile file) throws IOException {

        FileInputStream input1 = null;
        FileInputStream input2 = null;

        try {
            input1 = new FileInputStream(this);
            input2 = new FileInputStream(file);

            return IOUtils.contentEquals(input1, input2);

        } finally {
            IOUtils.closeQuietly(input1);
            IOUtils.closeQuietly(input2);
        }
    }


}