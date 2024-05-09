package com.l3s.dronegpt.model

data class ExperimentParameters(
    var north: Int,
    var east: Int,
    var south: Int,
    var west: Int,
    var height: Int
)