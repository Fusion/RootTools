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

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.ScrollView;
import android.widget.TextView;

public class SanityCheckRootTools extends Activity {
    private ScrollView mScrollView;
    private TextView mTextView;
    private ProgressDialog mPDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mTextView = new TextView(this);
        mTextView.setText("");
        mScrollView = new ScrollView(this);
        mScrollView.addView(mTextView);
        setContentView(mScrollView);

        // Great the user with our version number
        String version = "?";
        try {
            PackageInfo packageInfo =
                    this.getPackageManager().getPackageInfo(this.getPackageName(), 0);
            version = packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
        }

        print("SanityCheckRootTools v " + version + "\n\n");
        try {
			if (false == RootTools.isAccessGiven()) {
			    print("ERROR: No root access to this device.\n");
			    return;
			}
		} catch (Exception e) {
		    print("ERROR: could not determine root access to this device.\n");
		    return;
		}

        // Display infinite progress bar
        mPDialog = new ProgressDialog(this);
        mPDialog.setCancelable(false);
        mPDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);

        new SanityCheckThread(this, new TestHandler()).start();
    }

    protected void print(CharSequence text) {
        mTextView.append(text);
        mScrollView.post(new Runnable() {
            public void run() {
                mScrollView.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });
    }

    // Run our long-running tests in their separate thread so as to
    // not interfere with proper rendering.
    private class SanityCheckThread extends Thread {
        private Handler mHandler;

        public SanityCheckThread(Context context, Handler handler) {
            mHandler = handler;
        }

        public void run() {
            visualUpdate(TestHandler.ACTION_SHOW, null);

            // First test: Install a binary file for future use
            // if it wasn't already installed.
            /*
            visualUpdate(TestHandler.ACTION_PDISPLAY, "Installing binary if needed");
            if(false == RootTools.installBinary(mContext, R.raw.nes, "nes_binary")) {
                visualUpdate(TestHandler.ACTION_HIDE, "ERROR: Failed to install binary. Please see log file.");
                return;
            }
            */

            visualUpdate(TestHandler.ACTION_PDISPLAY, "Testing df");
            long spaceValue = RootTools.getSpace("/data");
            visualUpdate(TestHandler.ACTION_DISPLAY, "[ Checking /data partition size]\n");
            visualUpdate(TestHandler.ACTION_DISPLAY, spaceValue + "k\n\n");

            visualUpdate(TestHandler.ACTION_PDISPLAY, "Testing sendShell() w/ return array");
            try {
                List<String> response = RootTools.sendShell("ls /", InternalVariables.timeout);
                visualUpdate(TestHandler.ACTION_DISPLAY, "[ Listing of / (passing a List)]\n");
                for (String line : response) {
                    visualUpdate(TestHandler.ACTION_DISPLAY, line + "\n");
                }
            } catch (IOException e) {
                visualUpdate(TestHandler.ACTION_HIDE, "ERROR: " + e);
                return;
            } catch (RootToolsException e) {
                visualUpdate(TestHandler.ACTION_HIDE, "DEV-DEFINED ERROR: " + e);
                return;
            } catch (TimeoutException e) {
                visualUpdate(TestHandler.ACTION_HIDE, "Timeout.. " + e);
                return;
			}

            visualUpdate(TestHandler.ACTION_PDISPLAY, "Testing sendShell() w/ callbacks");
            try {
                visualUpdate(TestHandler.ACTION_DISPLAY, "\n[ Listing of / (callback)]\n");
                RootTools.Result result = new RootTools.Result() {
                    @Override
                    public void process(String line) throws Exception {
                        visualUpdate(TestHandler.ACTION_DISPLAY, line + "\n");
                    }

                    @Override
                    public void onFailure(Exception ex) {
                        visualUpdate(TestHandler.ACTION_HIDE, "ERROR: " + ex);
                        setError(1);
                    }

                    @Override
                    public void onComplete(int diag) {
                        visualUpdate(TestHandler.ACTION_DISPLAY, "------\nDone.\n");
                    }

					@Override
					public void processError(String line) throws Exception {
                        visualUpdate(TestHandler.ACTION_DISPLAY, line + "\n");						
					}
                };
                RootTools.sendShell("ls /", result, InternalVariables.timeout);
                if (0 != result.getError())
                    return;
            } catch (IOException e) {
                visualUpdate(TestHandler.ACTION_HIDE, "ERROR: " + e);
                return;
            } catch (RootToolsException e) {
                visualUpdate(TestHandler.ACTION_HIDE, "DEV-DEFINED ERROR: " + e);
                return;
            } catch (TimeoutException e) {
                visualUpdate(TestHandler.ACTION_HIDE, "Timeout.. " + e);
                return;
			}

            visualUpdate(TestHandler.ACTION_PDISPLAY, "Testing sendShell() for multiple commands");
            try {
                visualUpdate(TestHandler.ACTION_DISPLAY, "\n[ ps + ls + date / (callback)]\n");
                RootTools.Result result = new RootTools.Result() {
                    @Override
                    public void process(String line) throws Exception {
                        visualUpdate(TestHandler.ACTION_DISPLAY, line + "\n");
                    }

                    @Override
                    public void onFailure(Exception ex) {
                        visualUpdate(TestHandler.ACTION_HIDE, "ERROR: " + ex);
                        setError(1);
                    }

                    @Override
                    public void onComplete(int diag) {
                        visualUpdate(TestHandler.ACTION_DISPLAY, "------\nDone.\n");
                    }

					@Override
					public void processError(String line) throws Exception {
                        visualUpdate(TestHandler.ACTION_DISPLAY, line + "\n");						
					}

                };
                RootTools.sendShell(
                        new String[]{
                                "echo \"* PS:\"",
                                "ps",
                                "echo \"* LS:\"",
                                "ls",
                                "echo \"* DATE:\"",
                                "date"},
                        2000,
                        result,
                        InternalVariables.timeout
                );
                if (0 != result.getError())
                    return;
            } catch (IOException e) {
                visualUpdate(TestHandler.ACTION_HIDE, "ERROR: " + e);
            } catch (RootToolsException e) {
                visualUpdate(TestHandler.ACTION_HIDE, "DEV-DEFINED ERROR: " + e);
            } catch (TimeoutException e) {
                visualUpdate(TestHandler.ACTION_HIDE, "Timeout.. " + e);
                return;
			}

            visualUpdate(TestHandler.ACTION_PDISPLAY, "All tests complete.");
            visualUpdate(TestHandler.ACTION_HIDE, null);
        }

        private void visualUpdate(int action, String text) {
            Message msg = mHandler.obtainMessage();
            Bundle bundle = new Bundle();
            bundle.putInt(TestHandler.ACTION, action);
            bundle.putString(TestHandler.TEXT, text);
            msg.setData(bundle);
            mHandler.sendMessage(msg);
        }
    }

    private class TestHandler extends Handler {
        static final public String ACTION = "action";
        static final public int ACTION_SHOW = 0x01;
        static final public int ACTION_HIDE = 0x02;
        static final public int ACTION_DISPLAY = 0x03;
        static final public int ACTION_PDISPLAY = 0x04;
        static final public String TEXT = "text";

        public void handleMessage(Message msg) {
            int action = msg.getData().getInt(ACTION);
            String text = msg.getData().getString(TEXT);

            switch (action) {
                case ACTION_SHOW:
                    mPDialog.show();
                    mPDialog.setMessage("Running Root Library Tests...");
                    break;
                case ACTION_HIDE:
                    if (null != text)
                        print(text);
                    mPDialog.hide();
                    break;
                case ACTION_DISPLAY:
                    print(text);
                    break;
                case ACTION_PDISPLAY:
                    mPDialog.setMessage(text);
                    break;
            }
        }
    }
}
