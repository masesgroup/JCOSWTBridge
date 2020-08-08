/*
 *  MIT License
 *
 *  Copyright (c) 2020 MASES s.r.l.
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package org.mases.jcobridge.swt;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.swt.SWTException;
import org.eclipse.swt.internal.win32.OS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.mases.jcobridge.IJCGraphicContainer;

public abstract class JCSWTGraphicContainer implements IJCGraphicContainer {
    long m_hwndHost;
    long m_nativeHwnd;
    protected Display m_display;
    protected Shell m_shell;
    Object m_graphicObject;

    Object m_syncObject = new Object();
    Thread m_thread;

    boolean useDiplay = false;

    /**
     * Initialize the graphic container
     * 
     * @param hwndHost The HWND of the hosting application
     */
    public void initiaize(long hwndHost) {
        m_hwndHost = hwndHost;
        m_thread = new Thread() {
            public void run() {
                try {
                    m_display = createDisplay();
                    m_shell = createGraphic();

                    if (m_shell == null)
                        throw new SWTException("Shell cannot be null.");

                    if (useDiplay) {
                        Field field = Display.class.getDeclaredField("hwndMessage");
                        field.setAccessible(true);
                        m_nativeHwnd = field.getLong(m_display);
                        long lStyle = OS.GetWindowLongPtr(m_nativeHwnd, OS.GWL_STYLE);
                        lStyle |= OS.WS_CHILD;
                        OS.SetWindowLongPtr(m_nativeHwnd, OS.GWL_STYLE, lStyle);
                        OS.SetParent(m_nativeHwnd, m_hwndHost);
                        OS.SetWindowPos(m_shell.handle, OS.HWND_TOP, 0, 0, 0, 0, OS.SWP_NOMOVE | OS.SWP_NOSIZE);
                        OS.SetWindowPos(m_nativeHwnd, m_hwndHost, 0, 0, 0, 0, OS.SWP_NOMOVE | OS.SWP_NOSIZE);
                        m_shell.open();
                    } else {
                        m_shell.open();
                        m_nativeHwnd = m_shell.handle;
                        long lStyle = OS.GetWindowLongPtr(m_nativeHwnd, OS.GWL_STYLE);
                        lStyle &= ~(OS.WS_POPUP | OS.WS_CAPTION | OS.WS_THICKFRAME | OS.WS_MINIMIZEBOX
                                | OS.WS_MAXIMIZEBOX | OS.WS_SYSMENU);
                        lStyle |= OS.WS_CHILD;
                        OS.SetWindowLongPtr(m_nativeHwnd, OS.GWL_STYLE, lStyle);
                        OS.SetParent(m_nativeHwnd, m_hwndHost);
                    }
                    executeLoop();
                } catch (Exception ex) {
                    ex.printStackTrace();
                } finally {
                    synchronized (m_syncObject) {
                        m_syncObject.notify();
                    }
                }
            }
        };
        m_thread.setName("JCSWTGraphicContainer");
        m_thread.start();
        synchronized (m_syncObject) {
            try {
                m_syncObject.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    protected Display createDisplay() {
        return new Display();
    }

    protected abstract Shell createGraphic();

    protected void setGraphicObject(Object object) {
        m_graphicObject = object;
    }

    protected void executeLoop() {
        while (!m_shell.isDisposed()) {
            if (!m_display.readAndDispatch()) {
                synchronized (m_syncObject) {
                    m_syncObject.notify();
                }
                m_display.sleep();
            }
        }
        m_display.dispose();
    }

    /**
     * Destroy the graphic container
     */
    public void destroy() {
        m_display.syncExec(new Runnable() {
            @Override
            public void run() {
                m_shell.setVisible(false);
                m_shell.dispose();
            }
        });
    }

    /**
     * Return if the graphic container implements embedding styles and all
     * management for window placement
     * <p>
     * On Windows this means to remove from the HWND: WS_CAPTION | WS_THICKFRAME |
     * WS_MINIMIZEBOX | WS_MAXIMIZEBOX | WS_SYSMENU and add the child behavior using
     * WS_CHILDWINDOW
     * 
     * @return True if the graphic container implements embedding styles and
     *         management
     */
    public boolean isRemoteManaged() {
        return true;
    }

    /**
     * Sets visibility and bounding rectangle
     * 
     * @param isVisible Set visibility
     * @param x         The x-axis value of the left side of the rectangle.
     * @param y         The y-axis value of the top side of the rectangle.
     * @param width     A positive number that represents the width of the
     *                  rectangle.
     * @param height    A positive number that represents the height of the
     *                  rectangle.
     */
    public void setSizeAndVisibility(boolean isVisible, int x, int y, int width, int height) {
        m_display.syncExec(new Runnable() {
            @Override
            public void run() {
                m_shell.setVisible(isVisible);
                m_shell.setBounds(x, y, width, height);
            }
        });
/*
        int flags = 0;
        flags = OS.SWP_NOSIZE;

        if (!OS.SetWindowPos(m_nativeHwnd, m_shell.handle, x, y, width, height, flags)) {
            throw new SWTException(
                    String.format("Error %d on SetWindowPos in setSizeAndVisibility", OS.GetLastError()));
        }*/
    }

    /**
     * Return if the graphic container implements management of MeasureOverride
     * 
     * @return True if the object manages MeasureOverride
     */
    public boolean hasMeasureOverride() {
        return false;
    }

    /**
     * Starts a MeasureOverride operation
     * 
     * @param width  The width constraint received
     * @param height The height constraint received
     */
    public void startMeasureOverride(int width, int height) {
    }

    /**
     * MeasureOverride Width
     * 
     * @return The width measured
     */
    public int getMeasureOverrideWidth() {
        return 0;
    }

    /**
     * MeasureOverride Height
     * 
     * @return The height measured
     */
    public int getMeasureOverrideHeight() {
        return 0;
    }

    /**
     * End MeasureOverride operation
     */
    public void endMeasureOverride() {
    }

    /**
     * Return if the graphic container implements management for WndProc
     * 
     * @return True if the object manages WndProc
     */
    public boolean hasWndProcHandler() {
        return false;
    }

    /**
     * Accesses the window process (handle) of the hosted child window
     * 
     * @param hwnd:    The window handle of the hosted window.
     * @param msg:     The message to act upon.
     * @param wParam:  Information that may be relevant to handling the message.
     *                 This is typically used to store small pieces of information,
     *                 such as flags.
     * @param lParam:  Information that may be relevant to handling the message.
     *                 This is typically used to reference an object.
     * @param handled: The handled value coming from the orginal call.
     * @return True if the event was handled
     */
    public boolean wndProcHandler(long hwnd, int msg, long wParam, long lParam, boolean handled) {
        return false;
    }

    /**
     * Get focus state
     * <p>
     * 
     * @return the focus state of the graphic container
     */
    public boolean hasFocus() {
        final AtomicBoolean retVal = new AtomicBoolean();
        m_display.syncExec(new Runnable() {
            @Override
            public void run() {
                retVal.set(m_shell.isFocusControl());
            }
        });
        return retVal.get();
    }

    /**
     * Retrieves the native handle to the hosting window
     * <p>
     * 
     * @return The native pointer
     */
    public long getNativeWindowHandle() {
        return m_nativeHwnd;
    }

    /**
     * The graphic {@link Object} hosted from the graphic container
     * <p>
     *
     * @return {@link Object} hosted
     */
    public Object getGraphicObject() {
        return m_graphicObject;
    }

}