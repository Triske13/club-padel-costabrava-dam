package com.clubpadelcostabrava.app.data

import java.io.IOException

class ApiException(val estado: Int, message: String) : IOException(message)