// Use an integer for version numbers
version = 1

android {
    namespace = "recloudstream.cineby"
}

cloudstream {
    description = "Watch movies and TV series from Cineby"
    authors = listOf("User")

    status = 1

    tvTypes = listOf("Movie", "TvSeries")
    iconUrl = "https://www.cineby.sc/favicon.ico"

    isCrossPlatform = true
}