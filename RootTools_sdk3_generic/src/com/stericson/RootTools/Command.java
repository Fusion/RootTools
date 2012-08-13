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

/*
 *Special thanks to Jeremy Lakeman for the following code and for teaching me something new.
 *
 *Stephen
 */

package com.stericson.RootTools;

import java.io.IOException;
import java.io.OutputStream;

public abstract class Command {
	final String command[];
	boolean finished = false;
	int exitCode;
	int id;

	public Command(int id, String... command) {
		this.command = command;
		this.id = id;
	}
	
	public String getCommand() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < command.length; i++) {
			sb.append(command[i]);
			sb.append('\n');
		}
		RootTools.log("Sending command(s): " + sb.toString());
		return sb.toString();
	}

	public void writeCommand(OutputStream out) throws IOException {
		out.write(getCommand().getBytes());
	}

	public abstract void output(int id, String line);
	
	public void commandFinished(int id)
	{
		RootTools.log("Command " + id + "finished.");
	}

	public void exitCode(int code) {
		synchronized (this) {
			exitCode = code;
			finished = true;
			commandFinished(id);
			this.notifyAll();
		}
	}
	
	public void terminated() {
		exitCode(-1);
		RootTools.log(getCommand() + " did not finish.");
	}
	
	// waits for this command to finish and returns the exit code
	public void waitForFinish() throws InterruptedException {
		synchronized (this) {
			while (!finished) {
				this.wait();
			}
		}
	}
	
	// waits for this command to finish and returns the exit code
	public int exitCode() throws InterruptedException {
		synchronized (this) {
			while (!finished) {
				this.wait();
			}
		}
		return exitCode;
	}
}