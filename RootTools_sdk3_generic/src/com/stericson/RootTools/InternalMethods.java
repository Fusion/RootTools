/* 
 * This file is part of the RootTools Project: http://code.google.com/p/roottools/
 *  
 * Copyright (c) 2012 Stephen Erickson, Chris Ravenscroft, Dominik Schuermann, Adam Shanks
 *  
 * This code is dual-licensed under the terms of the Apache License Version 2.0 and
 * the terms of the General Public License (GPL) Version 2.
 * You may use this code according to either of these licenses as is most appropriate
 * for your project on a case-by-case basis.
 * 
 * The terms of each license can be found in the root directory of this project's repository as well as at:
 * 
 * * http://www.apache.org/licenses/LICENSE-2.0
 * * http://www.gnu.org/licenses/gpl-2.0.txt
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under these Licenses is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See each License for the specific language governing permissions and
 * limitations under that License.
 */

package com.stericson.RootTools;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.TimeoutException;

//no modifier, this is package-private which means that no one but the library can access it.
class InternalMethods {

    //--------------------
    //# Internal methods #
    //--------------------

    protected boolean returnPath() throws TimeoutException {
        File tmpDir = new File("/data/local/tmp");
        try {
            if (!tmpDir.exists()) {
                RootTools.sendShell(new String[]{"mkdir /data/local/tmp"}, 0, InternalVariables.timeout);
            }
            
            InternalVariables.path = new HashSet<String>();
            //Try to read from the file.
            LineNumberReader lnr = null;
            RootTools.sendShell(new String[]{"dd if=/init.rc of=/data/local/tmp/init.rc",
                    "chmod 0777 /data/local/tmp/init.rc"}, 0, InternalVariables.timeout);
            lnr = new LineNumberReader(new FileReader("/data/local/tmp/init.rc"));
            String line;
            while ((line = lnr.readLine()) != null) {
                RootTools.log(line);
                if (line.contains("export PATH")) {
                    int tmp = line.indexOf("/");
                    InternalVariables.path = new HashSet<String>(Arrays.asList(line.substring(tmp).split(":")));
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            if (RootTools.debugMode) {
                RootTools.log("Error: " + e.getMessage());
                e.printStackTrace();
            }
            return false;
        }
    }

    protected ArrayList<Mount> getMounts() throws FileNotFoundException, IOException {
        LineNumberReader lnr = null;
        try {
            lnr = new LineNumberReader(new FileReader("/proc/mounts"));
            String line;
            ArrayList<Mount> mounts = new ArrayList<Mount>();
            while ((line = lnr.readLine()) != null) {

                RootTools.log(line);

                String[] fields = line.split(" ");
                mounts.add(new Mount(
                        new File(fields[0]), // device
                        new File(fields[1]), // mountPoint
                        fields[2], // fstype
                        fields[3] // flags
                ));
            }
            return mounts;
        } finally {
            //no need to do anything here.
        }
    }

    protected ArrayList<Symlink> getSymLinks() throws FileNotFoundException, IOException {
        LineNumberReader lnr = null;
        try {
            lnr = new LineNumberReader(new FileReader("/data/local/symlinks.txt"));
            String line;
            ArrayList<Symlink> symlink = new ArrayList<Symlink>();
            while ((line = lnr.readLine()) != null) {

                RootTools.log(line);

                String[] fields = line.split(" ");
                symlink.add(new Symlink(
                        new File(fields[fields.length - 3]), // file
                        new File(fields[fields.length - 1]) // SymlinkPath
                ));
            }
            return symlink;
        } finally {
            //no need to do anything here.
        }
    }

    protected Permissions getPermissions(String line) {

    	String[] lineArray = line.split(" ");
    	String rawPermissions = lineArray[0];
    	
    	if (rawPermissions.length() == 10 && (rawPermissions.charAt(0) == '-' || rawPermissions.charAt(0) == 'd'
    			|| rawPermissions.charAt(0) == 'l') && (rawPermissions.charAt(1) == '-' || rawPermissions.charAt(1) == 'r') 
    			&& (rawPermissions.charAt(2) == '-' || rawPermissions.charAt(2) == 'w'))
    	{	
	    	RootTools.log(rawPermissions);
	    	
	    	Permissions permissions = new Permissions();
	    	
	    	permissions.setType(rawPermissions.substring(0, 1));
	
	    	RootTools.log(permissions.getType());
	
	    	permissions.setUserPermissions(rawPermissions.substring(1, 4));
	    	
	    	RootTools.log(permissions.getUserPermissions());
	    	
	    	permissions.setGroupPermissions(rawPermissions.substring(4, 7));
	    	
	    	RootTools.log(permissions.getGroupPermissions());
	
	    	permissions.setOtherPermissions(rawPermissions.substring(7, 10));
	    	
	    	RootTools.log(permissions.getOtherPermissions());
	
	    	
	    	String finalPermissions;
	    	finalPermissions = Integer.toString(parsePermissions(permissions.getUserPermissions()));
	    	finalPermissions += Integer.toString(parsePermissions(permissions.getGroupPermissions()));
	    	finalPermissions += Integer.toString(parsePermissions(permissions.getOtherPermissions()));
	    	
			permissions.setPermissions(Integer.parseInt(finalPermissions));
	    	
	        return permissions;
    	}
    	
    	return null;
    }
    
    protected int parsePermissions(String permission)
    {
    	int tmp;
    	if (permission.charAt(0) == 'r')
    		tmp = 4;
    	else
    		tmp = 0;

    	RootTools.log("permission " + tmp);
    	RootTools.log("character " + permission.charAt(0));
    	
    	if (permission.charAt(1) == 'w')
    		tmp = tmp + 2;
    	else
    		tmp = tmp + 0;

    	RootTools.log("permission " + tmp);
    	RootTools.log("character " + permission.charAt(1));

    	if (permission.charAt(2) == 'x')
    		tmp = tmp + 1;
    	else
    		tmp = tmp + 0;

    	RootTools.log("permission " + tmp);
    	RootTools.log("character " + permission.charAt(2));

    	return tmp;
    }

    /*
     * @return long Size, converted to kilobytes (from xxx or xxxm or xxxk etc.)
     */
    protected long getConvertedSpace(String spaceStr) {
    	try {
	        double multiplier = 1.0;
	        char c;
	        StringBuffer sb = new StringBuffer();
	        for (int i = 0; i < spaceStr.length(); i++) {
	            c = spaceStr.charAt(i);
	            if (!Character.isDigit(c) && c != '.') {
	                if (c == 'm' || c == 'M') {
	                    multiplier = 1024.0;
	                } else if (c == 'g' || c == 'G') {
	                    multiplier = 1024.0 * 1024.0;
	                }
	                break;
	            }
	            sb.append(spaceStr.charAt(i));
	        }
	        return (long) Math.ceil(Double.valueOf(sb.toString()) * multiplier);
    	}
    	catch (Exception e) 
    	{
    		return -1;
    	}
    }
}
