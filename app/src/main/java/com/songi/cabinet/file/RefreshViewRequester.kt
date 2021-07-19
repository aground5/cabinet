package com.songi.cabinet.file

class RefreshViewRequester {
    private var TAG = "RefreshViewRequester"

    var listener = mutableListOf<OnRequestListener>()

    fun request(tag: String) {
        for (i in listener) {
            i.onRequested(tag)
        }
    }

    fun addOnRequestListener(listener : (String) -> Unit) {
        this.listener.add(object : OnRequestListener{
            override fun onRequested(tag: String) {
                listener(tag)
            }
        })
    }

    interface OnRequestListener {
        fun onRequested(tag: String)
    }
}