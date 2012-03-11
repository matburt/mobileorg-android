git submodule init
git submodule update

android update project --path . --library libs/ActionBarSherlock/library/ -l libs/locale/

android update lib-project --path libs/locale/
android update lib-project --path libs/ActionBarSherlock/library/
